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

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class DBSchemaStore2Test extends DBTestBase {

    @Test
    void test() {
        val store = new DBSchemaStore2(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                       bundle.createRelatedObjectDao(StoredFieldSchema.class));
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

        val schema = store.get(sId).orElse(null);
        assertNotNull(schema);
        assertEquals(5, schema.getFields().size());
        assertEquals(FieldType.STRING, schema.getFields().get(0).getType());
        assertEquals(FieldType.BOOLEAN, schema.getFields().get(1).getType());
        assertEquals(FieldType.DATE, schema.getFields().get(2).getType());
        assertEquals(FieldType.LOCATION, schema.getFields().get(3).getType());
        assertEquals(FieldType.NUMBER, schema.getFields().get(4).getType());
    }
}