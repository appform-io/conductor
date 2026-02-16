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

package io.appform.conductor.core.ticketmanagement;

import lombok.Value;

import java.util.List;

/**
 * Result set for a list query. Returns latest results first.
 */
@Value
@SuppressWarnings("javs:S6548")
public class TicketSkeletonListResult {
    public static final TicketSkeletonListResult EMPTY = new TicketSkeletonListResult(List.of(), null);

    List<TicketSkeleton> results;
    String next;
}
