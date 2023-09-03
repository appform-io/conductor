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

package io.appform.conductor.server.ui.views.actions.fragments;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionScope;
import io.appform.conductor.model.actions.impl.WebhookAction;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WebHookActionFragment extends TemplateView {
    Set<WebhookAction.CallType> availableCallTypes = EnumSet.allOf(WebhookAction.CallType.class);
    Set<WebhookAction.MimeType> availableMimeTypes = EnumSet.allOf(WebhookAction.MimeType.class);
    Set<WebhookAction.CallMode> availableCallModes = EnumSet.allOf(WebhookAction.CallMode.class);
    ActionScope scope;
    Action currentAction;

    public WebHookActionFragment(ActionScope scope, Action currentAction) {
        super("templates/actions/fragments/webhook.hbs");
        this.scope = scope;
        this.currentAction = currentAction;
    }
}
