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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * DB representation for {@link org.hibernate.annotations.Comment}
 */
@Entity
@Table(name = StoredComment.TICKET_COMMENTS_TABLE_NAME,
    indexes = {
        @Index(name = "idx_comment_for_ticket", columnList = "ticket_id, deleted"),
        @Index(name = "idx_replies_for_ticket_comment", columnList = "ticket_id, reply_to_id, deleted"),
    })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredComment implements Serializable {
    public static final String TICKET_COMMENTS_TABLE_NAME = "ticket_comments";

   @Serial
   private static final long serialVersionUID = -5044362079995936712L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "comment_id", unique = true)
    private String commentId;

    @Column
    private String author;

    @Column(columnDefinition = "longtext")
    private String content;

    @Column(name = "reply_to_id")
    private String replyToId;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredComment that = (StoredComment) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
