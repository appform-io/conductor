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

package io.appform.conductor.server.schemamanagement.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.fields.BooleanFieldSchema;
import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchema;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static io.appform.conductor.server.utils.ConductorServerUtils.configureMapper;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class DBSchemaStoreTest extends DBTestBase {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setup() {
        configureMapper(MAPPER);
    }

    @Test
    void testCRU() {
        val store = new DBSchemaStore(bundle.createRelatedObjectDao(StoredSchema.class), MAPPER);
        val res = store.create("Test",
                               "TestSchema",
                               List.of(new BooleanFieldSchema("f1",
                                                              "f1",
                                                              "F1",
                                                              "Test field",
                                                              true,
                                                              null,
                                                              null,
                                                              null,
                                                              false,
                                                              new Date(),
                                                              new Date(),
                                                              false)));
        assertTrue(res.isPresent());
        //Now let's update description
        val schemaId = res.get().getId();
        val originalVersion = res.get().getVersion();
        assertEquals("test", schemaId);
        assertEquals(1, originalVersion);
        assertEquals("Test field changed",
                     store.updateDescription(schemaId, originalVersion, "Test field changed")
                             .map(Schema::getDescription)
                             .orElse(null));
        //There is no active version, so get should return empty
        assertTrue(store.get(schemaId).isEmpty());
        //Get by version should work
        assertEquals(SchemaState.INACTIVE,
                     store.getVersion(schemaId, originalVersion).map(Schema::getState).orElse(null));
        //Let's update state
        assertEquals("Test field changed",
                     store.updateDescription(schemaId, originalVersion, "Test field changed")
                             .map(Schema::getDescription)
                             .orElse(null));
        //Let's update state now.
        //First try to set required as ACTIVE, so it would return empty as schema is inactive
        try {
            store.updateState(schemaId, originalVersion, SchemaState.ACTIVE, SchemaState.INACTIVE);
            fail("Should have thrown");
        }
        catch (Exception e) {
            if (e instanceof ConductorException ce) {
                assertEquals(
                        "Error updating schema version test/1. Operation attempted: State update from ACTIVE to " +
                                "INACTIVE",
                        ce.getMessage());
            }
            else {
                fail("Unexpected exception " + e);
            }
        }
        //Now let's update properly
        val updatedStateRes = store.updateState(schemaId, originalVersion, SchemaState.INACTIVE, SchemaState.ACTIVE);
        assertEquals(SchemaState.ACTIVE, updatedStateRes.map(Schema::getState).orElse(null));
        assertEquals(originalVersion, updatedStateRes.map(Schema::getVersion).orElse(-1L)); //Ensure no version update
        assertTrue(store.get(schemaId).isPresent()); //Now it should be available

        //Now let's update a field
        val newVersionRes = store.updateFields(schemaId, originalVersion, List.of(new BooleanFieldSchema("f2",
                                                                                                         "f2",
                                                                                                         "F2",
                                                                                                         "Test field 2",
                                                                                                         true,
                                                                                                         null,
                                                                                                         null,
                                                                                                         null,
                                                                                                         false,
                                                                                                         new Date(),
                                                                                                         new Date(),
                                                                                                         false)));
        assertTrue(newVersionRes.isPresent());
        //Ensure new version has been created
        assertEquals(originalVersion + 1, newVersionRes.map(Schema::getVersion).orElse(-1L));
        assertEquals("f2", newVersionRes.map(schema -> schema.getFields().get(0).getId()).orElse(null));
    }

    @Test
    void testMultiple() {
        val store = new DBSchemaStore(bundle.createRelatedObjectDao(StoredSchema.class), MAPPER);
        IntStream.rangeClosed(1, 100)
                .forEach(i -> {
                    val res = store.create("Test-" + i,
                                           "TestSchema",
                                           List.of(new BooleanFieldSchema("f" + i,
                                                                          "f" + i,
                                                                          "F" + i,
                                                                          "Test field " + i,
                                                                          true,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          false,
                                                                          new Date(),
                                                                          new Date(),
                                                                          false)));
                    assertTrue(res.isPresent());
                });
        IntStream.rangeClosed(1, 100)
                .forEach(i -> {
                    val newVersionRes = store.updateFields("test_" + i,
                                                           1,
                                                           List.of(new BooleanFieldSchema("f2",
                                                                                          "f2",
                                                                                          "F2",
                                                                                          "Test field 2",
                                                                                          true,
                                                                                          null,
                                                                                          null,
                                                                                          null,
                                                                                          false,
                                                                                          new Date(),
                                                                                          new Date(),
                                                                                          false)));
                    assertEquals(2, newVersionRes.map(Schema::getVersion).orElse(-1L));
                    assertEquals("f2", newVersionRes.map(schema -> schema.getFields().get(0).getId()).orElse(null));
                });
    }

}