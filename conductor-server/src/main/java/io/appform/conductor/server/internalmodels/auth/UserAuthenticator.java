/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.internalmodels.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.appform.conductor.model.usermgmt.*;
import io.appform.conductor.server.config.AuthConfig;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.SessionStore;
import io.appform.conductor.server.usermanagement.UserPasswordAuthStore;
import io.appform.conductor.server.usermanagement.UserStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.AesKey;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.appform.conductor.server.utils.ConductorServerUtils.errorMessage;

/**
 * Handles authentication requests
 */
@Slf4j
public class UserAuthenticator {
    private final UserStore userStore;
    private final GroupStore groupStore;
    private final UserPasswordAuthStore passwordAuthStore;
    private final SessionStore sessionStore;

    private final AuthConfig authConfig;

    @Inject
    public UserAuthenticator(
            UserStore userStore,
            GroupStore groupStore,
            UserPasswordAuthStore passwordAuthStore,
            SessionStore sessionStore,
            AuthConfig authConfig) {
        this.userStore = userStore;
        this.groupStore = groupStore;
        this.passwordAuthStore = passwordAuthStore;
        this.sessionStore = sessionStore;
        this.authConfig = authConfig;
    }

    /**
     * Authenticate a user or a session based onthe provided data
     * @param authData The data to be validated
     * @return A user session or empty
     */
    public Optional<UserSession> authenticate(final UserAuthData authData) {
        return authData.accept(new AuthenticationVisitor(userStore,
                                                         groupStore,
                                                         passwordAuthStore,
                                                         sessionStore,
                                                         authConfig));
    }

    private record AuthenticationVisitor(UserStore userStore,
                                         GroupStore groupStore,
                                         UserPasswordAuthStore passwordAuthStore,
                                         SessionStore sessionStore,
                                         AuthConfig authConfig) implements UserAuthDataVisitor<Optional<UserSession>> {

        private static final EnumSet<UserState> TERMINAL_USER_STATES
                = EnumSet.of(UserState.BLACKLISTED, UserState.EXITED, UserState.DELETED);

        @Override
        public Optional<UserSession> visit(PasswordAuthData passwordAuthData) {
            val userId = passwordAuthData.getUserId();
            val password = passwordAuthData.getPassword();
            val user = userStore
                    .getById(userId);
            val passwordData = user
                    .flatMap(userData -> passwordAuthStore.get(userData.getId()))
                    .orElse(null);
            if (null == passwordData) {
                log.error("No valid user found for userID: {}", userId);
                return Optional.empty();
            }
            val passwordVerified = matchHash(userId, password, passwordData.getPassword());
            val updatedPasswordData = passwordAuthStore.update(userId, passwordDetails -> {
                        val attempts = passwordDetails.getFailedPasswordAttempts() + 1;
                        if (passwordVerified) {
                            passwordDetails.setFailedPasswordAttempts(0);
                        }
                        else {
                            passwordDetails.setFailedPasswordAttempts(attempts);
                        }
                        return passwordDetails;
                    })
                    .orElse(null);
            if (null != updatedPasswordData) {
                if (updatedPasswordData.getFailedPasswordAttempts() > 3) {
                    userStore.updateState(userId, UserState.LOCKED);
                    log.warn("User {} has been locked due to too many failed password attempts.", userId);
                }
                else {
                    log.error("Password validation failure for user {}. [attempts: {}]",
                              userId, updatedPasswordData.getFailedPasswordAttempts());
                }
            }
            else {
                log.warn("No password info found for user: {}", userId);
            }
            if (passwordVerified) {
                return user
                        .filter(AuthenticationVisitor::isUserActionable)
                        .flatMap(this::createSession);
            }
            return Optional.empty();
        }

        @Override
        public Optional<UserSession> visit(UserTokenAuthData tokenData) {

            val consumer = new JwtConsumerBuilder()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    .setExpectedIssuer("Conductor")
                    .setExpectedAudience(UserType.HUMAN.name(), UserType.SYSTEM.name())
                    .setVerificationKey(verificationKey(authConfig))
                    .setJweContentEncryptionAlgorithmConstraints(
                            new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                                                     ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256))
                    .build();
            val token = tokenData.getToken();
            try {
                val jwtClaims = consumer.processToClaims(token);
                val userId = jwtClaims.getSubject();
                val sessionId = jwtClaims.getJwtId();
                return userStore.getById(userId)
                        .filter(AuthenticationVisitor::isUserActionable)
                        .flatMap(userSummary -> sessionStore.getById(userId, sessionId)
                                .map(sessionDetails -> new UserSession(
                                        new User(userSummary,
                                                 groupStore.findGroupsForUser(userSummary.getId()),
                                                 Set.of()),
                                        sessionDetails.getId(),
                                        sessionDetails.getState(),
                                        sessionDetails.getType(),
                                        sessionDetails.getExpiry(),
                                        token)));
            }
            catch (InvalidJwtException e) {
                log.error("JWT validation failure for token: " + token + " error: " + errorMessage(e));
            }
            catch (MalformedClaimException e) {
                log.error("Error reading user id/session id from token " + token + ": " + errorMessage(e));
            }
            return Optional.empty();
        }

        @SneakyThrows
        private String tokenFromSession(final UserSessionDetails session, UserSummary userSummary) {
            val claims = new JwtClaims();
            claims.setIssuer("Conductor");
            claims.setAudience(userSummary.getType().name());
            claims.setSubject(userSummary.getId());
            claims.setJwtId(session.getId());
            claims.setIssuedAtToNow();
            if (null != session.getExpiry()) {
                claims.setExpirationTime(NumericDate.fromMilliseconds(session.getExpiry().getTime()));
            }
            val jwe = new JsonWebEncryption();
            jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
            jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512);
            jwe.setKey(verificationKey(authConfig));
            jwe.setPayload(claims.toJson());
            return jwe.getCompactSerialization();
        }

        private static boolean isUserActionable(final UserSummary userDetails) {
            return null != userDetails
                    && !TERMINAL_USER_STATES.contains(userDetails.getState());
        }

        private static boolean matchHash(String userId, String s, String hash) {
            val res = BCrypt.verifyer().verify(s.toCharArray(), hash);
            if (!res.verified) {
                //TODO::UPDATE RATE LIMIT COUNTER
                log.warn("Password validation failure for: {}. Error: {}", userId, res);
            }
            return res.verified;
        }

        private Optional<UserSession> createSession(final UserSummary userSummary) {
            val user = new User(userSummary, groupStore.findGroupsForUser(userSummary.getId()), Set.of());
            val sessionType = switch (userSummary.getType()) {
                case HUMAN -> SessionType.DYNAMIC;
                case SYSTEM -> SessionType.STATIC;
            };
            val defaultSessionDuration = Objects.requireNonNullElse(authConfig.getSessionTime(), Duration.ofDays(30));
            val expiry = switch (sessionType) {
                case DYNAMIC -> Date.from(Instant.now().plus(defaultSessionDuration));
                case STATIC -> null;
            };
            return sessionStore.create(userSummary.getId(), sessionType, expiry)
                    .map(sessionDetails -> new UserSession(user,
                                                           sessionDetails.getId(),
                                                           sessionDetails.getState(),
                                                           sessionType,
                                                           expiry,
                                                           tokenFromSession(sessionDetails, userSummary)));
        }
    }

    private static AesKey verificationKey(AuthConfig authConfig) {
        return new AesKey(authConfig.getSigningSecret().getBytes(StandardCharsets.UTF_8));
    }
}
