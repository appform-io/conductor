/*
 * Copyright (c) 2023 santanu
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

package io.appform.conductor.server.ui.views.common;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.server.utils.Pair;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.util.List;

/**
 * Returns table rows
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EventsListFragment extends TemplateView {
    List<Pair<Event, String>> events;
    String next;

    public EventsListFragment(List<Pair<Event, String>> events, String next) {
        super("templates/common/fragments/events-list.hbs");
        this.events = events;
        this.next = next;
    }
}
