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

package io.appform.conductor.server.skillmanagement;

import io.appform.conductor.model.skills.SkillDefinition;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.skill.*;
import io.appform.conductor.server.eventmanagement.events.user.UserSkillAssociatedEvent;
import io.appform.conductor.server.eventmanagement.events.user.UserSkillDisasocciatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Singleton
public class EventGeneratingSkillStore implements SkillStore {
    private final EventBus eventBus;
    private final SkillStore skillStore;

    @Inject
    public EventGeneratingSkillStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) SkillStore skillStore) {
        this.eventBus = eventBus;
        this.skillStore = skillStore;
    }

    @Override
    public Optional<SkillDefinition> createSkillDefinition(String name) {
        val res = skillStore.createSkillDefinition(name);
        res.ifPresent(skillDefinition -> eventBus.publish(new SkillCreatedEvent(skillDefinition.getId())));
        return res;
    }

    @Override
    public Optional<SkillDefinition> updateSkillDefinition(String skillId, String name) {
        val res = skillStore.updateSkillDefinition(skillId, name);
        res.ifPresent(skillDefinition -> eventBus.publish(new SkillUpdatedEvent(skillDefinition.getId())));
        return res;
    }

    @Override
    public Optional<SkillDefinition> addValueToSkillDefinition(String id, String value) {
        val res = skillStore.addValueToSkillDefinition(id, value);
        res.flatMap(skillDefinition -> skillDefinition.getValues()
                        .stream()
                        .filter(skillValue -> skillValue.getValue().equals(value))
                        .findFirst())
                .ifPresent(skillValue -> eventBus.publish(new SkillValueAddedEvent(skillValue.getSkillId(),
                                                                                   skillValue.getSkillValueId())));
        return res;
    }

    @Override
    public Optional<SkillDefinition> removeValueFromSkillDefinition(String id, String valueId) {
        val res = skillStore.removeValueFromSkillDefinition(id, valueId);
        res.ifPresent(skillDefinition -> eventBus.publish(new SkillValueRemovedEvent(id, valueId)));
        return res;
    }

    @Override
    public Optional<SkillDefinition> updateSkillValue(String id, String valueId, String value) {
        return skillStore.updateSkillValue(id, valueId, value);
    }

    @Override
    public Optional<SkillDefinition> readSkillDefinition(String id) {
        return skillStore.readSkillDefinition(id);
    }

    @Override
    public Optional<SkillValue> readSkillValue(String id, String valueId) {
        return skillStore.readSkillValue(id, valueId);
    }

    @Override
    public boolean deleteSkillDefinition(String id) {
        val res = skillStore.deleteSkillDefinition(id);
        if (res) {
            eventBus.publish(new SkillDeletedEvent(id));
        }
        return res;
    }

    @Override
    public List<SkillDefinition> listSkillDefinitions() {
        return skillStore.listSkillDefinitions();
    }

    @Override
    public List<SkillValue> listSkillValues() {
        return skillStore.listSkillValues();
    }

    @Override
    public boolean associateSkillWithUser(String userId, String skillId, String valueId) {
        val res = skillStore.associateSkillWithUser(userId, skillId, valueId);
        if (res) {
            eventBus.publish(new UserSkillAssociatedEvent(skillId, valueId, userId));
        }
        return res;
    }

    @Override
    public boolean disassociateSkillWithUser(String userId, String skillId, String valueId) {
        val res = skillStore.disassociateSkillWithUser(userId, skillId, valueId);
        if (res) {
            eventBus.publish(new UserSkillDisasocciatedEvent(skillId, valueId, userId));
        }
        return res;
    }

    @Override
    public List<SkillValue> listSkillsForUser(String userId) {
        return skillStore.listSkillsForUser(userId);
    }
}
