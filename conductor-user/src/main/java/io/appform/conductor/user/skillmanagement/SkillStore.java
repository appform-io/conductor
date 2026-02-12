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

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface SkillStore {
    Optional<SkillDefinition> createSkillDefinition(final String name);
    Optional<SkillDefinition> updateSkillDefinition(final String skillId, final String name);
    Optional<SkillDefinition> addValueToSkillDefinition(final String id, final String value);
    Optional<SkillDefinition> removeValueFromSkillDefinition(final String id, final String valueId);
    Optional<SkillDefinition> updateSkillValue(final String id, final String valueId, final String value);

    Optional<SkillDefinition> readSkillDefinition(final String id);

    Optional<SkillValue> readSkillValue(String id, String valueId);

    boolean deleteSkillDefinition(final String id);

    List<SkillDefinition> listSkillDefinitions();

    List<SkillValue> listSkillValues();

    boolean associateSkillWithUser(final String userId, final String skillId, final String valueId);

    boolean disassociateSkillWithUser(final String userId, final String skillId, final String valueId);

    List<SkillValue> listSkillsForUser(final String userId);

}
