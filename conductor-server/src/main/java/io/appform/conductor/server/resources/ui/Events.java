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

package io.appform.conductor.server.resources.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.server.eventmanagement.events.ReferredObjectType;
import io.appform.conductor.server.eventmanagement.query.EventFilters;
import io.appform.conductor.server.eventmanagement.query.ObjectReference;
import io.appform.conductor.server.ui.views.common.EventsListFragment;
import io.appform.conductor.server.utils.Pair;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static io.appform.conductor.server.utils.ConductorServerUtils.render;

/**
 * {@link Event} related functionality
 */
@Slf4j
@Path("/ui/events")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@PermitAll
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Events {
    private final EventStore eventStore;
    private final ObjectMapper mapper;

    @Path("/list/object/{objectType}/{objectId}")
    @GET
    public Response response(
            @Auth final ConductorUser user,
            @PathParam("objectType") @NotNull final ReferredObjectType type,
            @PathParam("objectId") @NotEmpty @Length(max = 255) final String objectId,
            @PathParam("next") @Length(max = 255) final String next) {
        val results = eventStore.list(
                EventFilters.builder()
                        .reference(new ObjectReference(type, objectId))
                        .build(),
                next,
                10);
        return render(new EventsListFragment(results.getResults()
                                                     .stream()
                                                     .map(event -> Pair.of(event, toString(event)))
                                                     .toList(),
                                             results.getNext()),
                      Map.of("next-pointer", List.of(results.getNext())));
    }

    @SneakyThrows
    private String toString(Event event) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
    }
}
