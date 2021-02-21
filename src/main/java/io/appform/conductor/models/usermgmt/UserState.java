/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.models.usermgmt;

import lombok.Getter;

/**
 * Denotes a user state. Can be used in validations for sessions etc
 */
public enum UserState {
    /**
     * User has been created but not activated by an administrator
     */
    CREATED("Created") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitCreated();
        }
    },

    /**
     * User has been active on the system
     */
    ACTIVE("Active") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitActive();
        }
    },

    /**
     * User has left the organisation
     */
    EXITED("Exited") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitExited();
        }
    },

    /**
     * User is in the organisation but not logged in for a while
     */
    INACTIVE("Inactive") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitInactive();
        }
    },

    /**
     * User has been permanently blocked from the system
     */
    BLACKLISTED("Blacklisted") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitBlacklisted();
        }
    },

    /**
     * User has been deleted, probably created by mistake
     */
    DELETED("Deleted") {
        @Override
        <T> T visit(UserStateVisitor<T> visitor) {
            return visitor.visitDeleted();
        }
    };

    @Getter
    private final String displayName;

    UserState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * This will be used to handle state specific options
     * @param visitor An implemented visitor of type {@link UserStateVisitor}
     * @param <T> Return type for {@link UserStateVisitor} implementation
     * @return Processed result from visitor
     */
    abstract <T> T visit(final UserStateVisitor<T> visitor);

    /**
     * Override this visitor to handle state specific operations like validations of transitions, login check etc
     * @param <T> Return type
     */
    public interface UserStateVisitor<T> {
        T visitCreated();

        T visitActive();

        T visitExited();

        T visitInactive();

        T visitBlacklisted();

        T visitDeleted();
    }
}
