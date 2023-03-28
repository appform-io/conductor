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
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.SchemaSummary;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

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
    @SneakyThrows
    void test() {
        val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                      bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                      MAPPER);
        val schemaSummary = store.create("Test", "Test Schema").orElse(null);
        assertNotNull(schemaSummary);
        val sId = schemaSummary.getId();
        assertEquals(schemaSummary, store.getSummary(sId).orElse(null));
        assertNotNull(store.addField(sId, new StringFieldSchema("f1",
                                                                "f1",
                                                                "F1",
                                                                "Test field 1",
                                                                true,
                                                                null,
                                                                null,
                                                                null,
                                                                false,
                                                                new Date(),
                                                                new Date(),
                                                                100,
                                                                null,
                                                                "Default Value")).orElse(null));
        assertNotNull(store.addField(sId, new BooleanFieldSchema("f2",
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
                                                                 false)).orElse(null));
        assertNotNull(store.addField(sId, new DateFieldSchema("f3",
                                                              "f3",
                                                              "F3",
                                                              "Test field 3",
                                                              true,
                                                              null,
                                                              null,
                                                              null,
                                                              false,
                                                              new Date(),
                                                              new Date(),
                                                              new Date())).orElse(null));
        assertNotNull(store.addField(sId, new LocationFieldSchema("f4",
                                                                  "f4",
                                                                  "F4",
                                                                  "Test field 4",
                                                                  true,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  false,
                                                                  new Date(),
                                                                  new Date(),
                                                                  5.0,
                                                                  -15.0))
                              .orElse(null));
        assertNotNull(store.addField(sId, new NumberFieldSchema("f5",
                                                                "f5",
                                                                "F5",
                                                                "Test field 5",
                                                                true,
                                                                null,
                                                                null,
                                                                null,
                                                                false,
                                                                new Date(),
                                                                new Date(),
                                                                100,
                                                                -10.0,
                                                                0.0))
                              .orElse(null));
        assertNotNull(store.addField(sId, new ChoiceFieldSchema("f6",
                                                                "f6",
                                                                "F6",
                                                                "Test field 6",
                                                                true,
                                                                null,
                                                                null,
                                                                null,
                                                                false,
                                                                new Date(),
                                                                new Date(),
                                                                List.of(new ChoiceFieldSchema.Option("cat", "Cat"),
                                                                        new ChoiceFieldSchema.Option("dog", "Dog")),
                                                                "cat"))
                              .orElse(null));

        val schema = store.get(sId).orElse(null);
        assertNotNull(schema);
        assertEquals(6, schema.getFields().size());
        assertEquals(FieldType.STRING, schema.getFields().get(0).getType());
        assertEquals(FieldType.BOOLEAN, schema.getFields().get(1).getType());
        assertEquals(FieldType.DATE, schema.getFields().get(2).getType());
        assertEquals(FieldType.LOCATION, schema.getFields().get(3).getType());
        assertEquals(FieldType.NUMBER, schema.getFields().get(4).getType());
        assertEquals(FieldType.CHOICE, schema.getFields().get(5).getType());
        assertEquals(2, ((ChoiceFieldSchema) schema.getFields().get(5)).getChoices().size());

        assertEquals("Changed",
                     store.updateDescription(sId, "Changed").map(SchemaSummary::getDescription).orElse(null));
        assertEquals(SchemaState.ACTIVE,
                     store.updateState(sId, SchemaState.ACTIVE).map(SchemaSummary::getState).orElse(null));

        assertTrue(store.deleteField(sId, schema.getFields().get(0).getId()));
        assertEquals(5, store.get(sId).map(r -> r.getFields().size()).orElse(-1));
        assertEquals("Changed", store.updateField(sId, schema.getFields().get(1).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, schema.getFields().get(2).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, schema.getFields().get(3).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, schema.getFields().get(4).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, schema.getFields().get(5).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
    }
}