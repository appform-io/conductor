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

package io.appform.conductor.server.ticketmanagement;

import com.google.common.net.MediaType;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.FlatGroupCountResponse;
import io.appform.conductor.model.ticket.analytics.TimeResolution;
import io.appform.conductor.model.ticket.analytics.TimeSeriesResponse;
import io.appform.conductor.model.ticket.comments.Attachment;
import io.appform.conductor.model.ticket.comments.Comment;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import lombok.NonNull;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A place to store {@link io.appform.conductor.model.ticket.TicketSummary}
 */
public interface TicketStore {

    Optional<TicketSkeleton> create(
            final String ticketId,
            final String title,
            final String description,
            final String workflowId,
            final String subjectId,
            final String ticketStateId,
            final TicketPriority priority,
            final List<TicketFieldData> fields);

    Optional<TicketSkeleton> read(String ticketId, boolean readFields);

    Optional<TicketSkeleton> update(
            final String ticketId,
            final UnaryOperator<TicketSkeleton> updater,
            final List<TicketFieldData> fields);

    default Optional<TicketSkeleton> updateState(
            final String ticketId,
            @NonNull final TicketState newState) {
        return update(ticketId,
                      ticket -> ticket.setTicketStateId(newState.getId()),
                      List.of());
    }

    default Optional<TicketSkeleton> changePriority(
            final String ticketId,
            @NonNull final TicketPriority newPriority) {
        return update(ticketId,
                      ticket -> ticket.setPriority(newPriority),
                      List.of());
    }

    default Optional<TicketSkeleton> assignToGroup(
            final String ticketId,
            @NonNull final String groupId) {
        return update(ticketId,
                      ticket -> ticket.setAssignedToGroupId(groupId),
                      List.of());
    }

    default Optional<TicketSkeleton> setField(
            final String ticketId,
            @NonNull TicketFieldData field) {
        return setFields(ticketId, List.of(field));
    }

    default Optional<TicketSkeleton> setFields(
            final String ticketId,
            @NonNull List<TicketFieldData> fields) {
        return update(ticketId, ticket -> ticket, fields);
    }

    default Optional<TicketSkeleton> assignToUser(
            final String ticketId,
            @NonNull final String userId) {
        return update(ticketId,
                      ticket -> ticket.setAssignedToGroupId(userId),
                      List.of());
    }

    Optional<TicketSkeleton> update(
            final String ticketId,
            final String title,
            final String description,
            final String subjectId,
            final String ticketStateId,
            final TicketPriority priority,
            final List<TicketFieldData> fields);

    TicketSkeletonListResult list(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema);

    FlatGroupCountResponse groupCount(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final Map<String, FieldSchema> relevantFieldSchema,
            final String ticketPropertyName);

    TimeSeriesResponse timeSeries(final List<TicketFilter> ticketFilters,
                                  final List<TicketFieldFilter> fieldFilters,
                                  final Map<String, FieldSchema> relevantFieldSchema,
                                  final TimeResolution resolution);

    Optional<Comment> addComment(
            final String ticketId,
            String commentId, final String comment,
            final String inReplyTo);

    List<Comment> listComments(
            final String ticketId,
            final int from,
            final int size);

    List<Comment> repliesToComment(
            final String ticketId,
            final String replyToId,
            final int from,
            final int size);

    Optional<Attachment> registerAttachment(
            final String ticketId,
            final String attchmentId,
            final MediaType type,
            final URL url,
            final long sizeInBytes,
            boolean encrypted);

    List<Attachment> listAttachments(final String ticketId,
                                     final int from,
                                     final int size);

    boolean deleteAttachment(final String ticketId,
                             final String attachmentId);
}
