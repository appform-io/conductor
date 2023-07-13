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

package io.appform.conductor.model.ticket.comments;

import lombok.Value;

import java.util.Date;

/**
 * Represents comment type in a ticket
 */
@Value
public class Comment {

    /**
     * Global comment ID
     */
    String id;

    /**
     *  Author of the comment
     */
    String author;

    /**
     * The text content
     */
    String content;

    /**
     * This message id to which this is a reply to
     */
    String replyToId;

    /**
     * True if comment has been deleted
     */
    boolean deleted;

    /**
     * Date when comment was created
     */
    Date created;

    /**
     * Date when comment was last updated
     */
    Date updated;
}
