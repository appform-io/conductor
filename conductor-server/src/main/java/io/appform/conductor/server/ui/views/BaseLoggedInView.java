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

package io.appform.conductor.server.ui.views;

import io.appform.conductor.model.usermgmt.User;
import lombok.Getter;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

/**
 * A view for logged in user
 */
@Getter
public abstract class BaseLoggedInView extends TemplateView {
    private final User currentUser;

    protected BaseLoggedInView(String templatePath, User currentUser) {
        super(templatePath);
        this.currentUser = currentUser;
    }
}
