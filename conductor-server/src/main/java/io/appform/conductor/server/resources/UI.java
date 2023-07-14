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

package io.appform.conductor.server.resources;

import io.appform.conductor.server.ui.views.HomeView;
import lombok.extern.slf4j.Slf4j;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 */
@Slf4j
@Path("/ui")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
public class UI {

    @GET
    public HomeView home() {
        return new HomeView();
    }
}
