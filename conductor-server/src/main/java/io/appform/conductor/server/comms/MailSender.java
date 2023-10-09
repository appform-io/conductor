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

package io.appform.conductor.server.comms;

import io.appform.conductor.server.config.MailConfig;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Sends email
 */
@Singleton
@Slf4j
public class MailSender {

    public record Attachment(String name, String data, String mimeType) {
    }

    public record Mail(List<String> emailIds, String subject, String body, List<Attachment> attachments) {
    }

    private final MailConfig mailConfig;
    private final Mailer mailer;

    @Inject
    public MailSender(@NonNull MailConfig mailConfig) {
        this.mailConfig = mailConfig;
        mailer = MailerBuilder
                .withSMTPServerHost(mailConfig.getSmtpServer())
                .withSMTPServerPort(mailConfig.getPort())
                .withSMTPServerUsername(mailConfig.getUsername())
                .withSMTPServerPassword(mailConfig.getPassword())
                .withTransportStrategy(mailConfig.isTls()
                                       ? TransportStrategy.SMTP_TLS
                                       : TransportStrategy.SMTP)
                .buildMailer();
    }

    @SneakyThrows //TODO
    public void send(final Mail mail) {
        val emails = mail.emailIds();
        if (emails.isEmpty()) {
            log.warn("No email ids specified for mail with subject: {}", mail.subject());
        }
        val mailBuilder = EmailBuilder.startingBlank()
                .toMultiple(emails)
                .withSubject(mail.subject())
                .withPlainText(mail.body()) //TODO::TEMPLATE
                .from(mailConfig.getFrom());
        Objects.requireNonNullElse(mail.attachments, List.<Attachment>of())
                .forEach(attachment -> mailBuilder.withAttachment(attachment.name(),
                                                                  attachment.data().getBytes(StandardCharsets.UTF_8),
                                                                  attachment.mimeType()));
        val email = mailBuilder.buildEmail();
        mailer.sendMail(email);
        log.debug("Mail subject: {}, Recipients: {}", mail.subject(), mail.emailIds);
    }
}
