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

package io.appform.conductor.server.skillmanagement.impl;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillDefinition;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillValue;
import io.appform.conductor.server.skillmanagement.impl.models.StoredUserSkillAssociation;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.skillmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBSkillStoreTest {
    @Test
    void testCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val skillStore = new DBSkillStore(
                bundle.createParentObjectDao(StoredSkillDefinition.class),
                bundle.createRelatedObjectDao(StoredSkillValue.class),
                bundle.createRelatedObjectDao(StoredUserSkillAssociation.class)
                );
        val skill = skillStore.createSkillDefinition("Specialization").orElse(null);
        assertNotNull(skill);
        {
            val updated = skillStore.addValueToSkillDefinition(skill.getId(), "Badminton").orElse(null);
            assertTrue(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Badminton")));
        }
        {
            val updated = skillStore.addValueToSkillDefinition(skill.getId(), "Cricket").orElse(null);
            assertTrue(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Badminton")));
            assertTrue(skillStore.readSkillValue(skill.getId(), "SPECIALIZATION_BADMINTON").isPresent());
            assertTrue(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Cricket")));
        }
        {
            val updated = skillStore.updateSkillValue(skill.getId(), "SPECIALIZATION_BADMINTON", "Tennis").orElse(null);
            assertFalse(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Badminton")));
            assertTrue(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Tennis")));
        }
        {
            val updated = skillStore.removeValueFromSkillDefinition(skill.getId(), "SPECIALIZATION_BADMINTON").orElse(null);
            assertFalse(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Tennis")));
            assertFalse(skillStore.readSkillValue(skill.getId(), "SPECIALIZATION_BADMINTON").isPresent());
            assertTrue(updated.getValues().stream().map(SkillValue::getValue).anyMatch(v -> v.equals("Cricket")));
        }
        {
            assertTrue(skillStore.deleteSkillDefinition(skill.getId()));
        }
        try {
            skillStore.addValueToSkillDefinition("Random id", "Badminton");
            fail("Should have thrown");
        }
        catch (ConductorException e) {
            assertEquals(ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR, e.getErrorCode());
        }
        IntStream.rangeClosed(1, 10)
                .forEach(i -> skillStore.createSkillDefinition("Skill_" + i));
        assertEquals(10, skillStore.list().size());
    }

    @Test
    void testUserAssoc(BalancedDBShardingBundle<TestConfig> bundle) {
        val skillStore = new DBSkillStore(
                bundle.createParentObjectDao(StoredSkillDefinition.class),
                bundle.createRelatedObjectDao(StoredSkillValue.class),
                bundle.createRelatedObjectDao(StoredUserSkillAssociation.class));
        val skill = skillStore.createSkillDefinition("Specialization").orElse(null);
        assertNotNull(skill);
        skillStore.addValueToSkillDefinition(skill.getId(), "Badminton");
        skillStore.addValueToSkillDefinition(skill.getId(), "Cricket");

        assertTrue(skillStore.associateSkillWithUser("TU", skill.getId(), "SPECIALIZATION_BADMINTON"));
        assertTrue(skillStore.listSkillsForUser("TU").stream().anyMatch(skillValue -> skillValue.getSkillValueId().equals("SPECIALIZATION_BADMINTON")));
        assertTrue(skillStore.disassociateSkillWithUser("TU", skill.getId(), "SPECIALIZATION_BADMINTON"));
        assertFalse(skillStore.listSkillsForUser("TU").stream().anyMatch(skillValue -> skillValue.getSkillValueId().equals("SPECIALIZATION_BADMINTON")));
        assertFalse(skillStore.associateSkillWithUser("TU", skill.getId(), "BLAH"));

    }
}