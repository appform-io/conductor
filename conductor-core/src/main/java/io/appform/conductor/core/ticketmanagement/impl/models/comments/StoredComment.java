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

package io.appform.conductor.core.ticketmanagement.impl.models.comments;

import io.appform.conductor.core.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
        @Index(name = "idx_ticket_id_reply_to_id", columnList = "ticket_id, reply_to_id"),
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
    @Column(name = "comment_id", nullable = false, unique = true, length = Constants.MAX_COMMENT_ID_LENGTH)
    private String commentId;

    @Column(name = "ticket_id", nullable = false, length = Constants.MAX_TICKET_ID_LENGTH)
    private String ticketId;

    @Column(name = "author", length = Constants.MAX_USER_ID_LENGTH)
    private String author;

    @Column(name = "content", columnDefinition = "text", length = Constants.MAX_COMMENT_LENGTH)
    private String content;

    @Column(name = "reply_to_id", length = Constants.MAX_TICKET_ID_LENGTH)
    private String replyToId;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
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
        return Objects.equals(getCommentId(), that.getCommentId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
