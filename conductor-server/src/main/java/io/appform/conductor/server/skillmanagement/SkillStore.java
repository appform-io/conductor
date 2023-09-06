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

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface SkillStore {
    Optional<SkillDefinition> createSkill(final String name);
    Optional<SkillDefinition> addValueToSkill(final String id, final String value);
    Optional<SkillDefinition> removeValueFromSkill(final String id, final String valueId);
    Optional<SkillDefinition> updateSkillValue(final String id, final String valueId, final String value);

    Optional<SkillDefinition> readSkill(final String id);

    boolean deleteSkill(final String id);

    List<SkillDefinition> list();
}
