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

package io.appform.conductor.server.ticketmanagement.impl.models.comments;

import com.google.common.net.MediaType;
import io.appform.conductor.server.utils.persistence.MediaTypeConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

/**
 * DB representation for {@link io.appform.conductor.model.ticket.comments.Attachment}
 */
@Entity
@Table(name = StoredAttachment.TICKET_ATTACHMENTS_TABLE_NAME,
    indexes = {
        @Index(name = "idx_attachment_for_ticket", columnList = "ticket_id, deleted"),
    })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredAttachment implements Serializable {
    public static final String TICKET_ATTACHMENTS_TABLE_NAME = "ticket_attachments";

    @Serial
    private static final long serialVersionUID = -5698876015229760326L;

    @Id
    @Column(name = "attachment_id", unique = true)
    private String attachmentId;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column
    private String creator;

    @SuppressWarnings("java:S1948")
    @Convert(converter = MediaTypeConverter.class)
    @Column
    private MediaType mediaType;

    @Column
    private URL url;

    @Column(name = "size_in_bytes")
    private long sizeInBytes;

    @Column
    boolean encrypted;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredAttachment that = (StoredAttachment) o;
        return Objects.equals(getAttachmentId(), that.getAttachmentId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
