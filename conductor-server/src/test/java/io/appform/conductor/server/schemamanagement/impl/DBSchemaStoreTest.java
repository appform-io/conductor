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
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.*;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;
import java.util.List;

import static io.appform.conductor.server.utils.ConductorServerUtils.configureMapper;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link DBSchemaStore}
 */
@Slf4j
@RelevantDBEntityPackages("io.appform.conductor.server.schemamanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBSchemaStoreTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setup() {
        configureMapper(MAPPER);
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("java:S5961")
    void test(BalancedDBShardingBundle<TestConfig> bundle) {
        val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                      bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                      MAPPER);
        val schemaSummary = store.create("Test", "Test Schema").orElse(null);
        assertNotNull(schemaSummary);
        val sId = schemaSummary.getId();
        assertEquals(schemaSummary, store.getSummary(sId).orElse(null));
        assertNotNull(store.addField(sId, sId + "-f1", new StringFieldSchema("f1",
                                                                  "f1",
                                                                  "F1",
                                                                  "Test field 1",
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  false,
                                                                  new Date(),
                                                                  new Date(),
                                                                  100,
                                                                  null,
                                                                  "Default FieldValue")).orElse(null));
        assertNotNull(store.addField(sId, sId + "-f2", new BooleanFieldSchema("f2",
                                                                   "f2",
                                                                   "F2",
                                                                   "Test field 2",
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   false,
                                                                   new Date(),
                                                                   new Date(),
                                                                   false)).orElse(null));
        assertNotNull(store.addField(sId, sId + "-f3", new DateFieldSchema("f3",
                                                                "f3",
                                                                "F3",
                                                                "Test field 3",
                                                                null,
                                                                null,
                                                                null,
                                                                false,
                                                                new Date(),
                                                                new Date(),
                                                                new Date())).orElse(null));
        assertNotNull(store.addField(sId, sId + "-f4", new LocationFieldSchema("f4",
                                                                    "f4",
                                                                    "F4",
                                                                    "Test field 4",
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    false,
                                                                    new Date(),
                                                                    new Date(),
                                                                    5.0,
                                                                    -15.0))
                                                                        .orElse(null));
        assertNotNull(store.addField(sId, sId + "-f5", new NumberFieldSchema("f5",
                                                                  "f5",
                                                                  "F5",
                                                                  "Test field 5",
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
        assertNotNull(store.addField(sId, sId + "-f6", new ChoiceFieldSchema("f6",
                                                                  "f6",
                                                                  "F6",
                                                                  "Test field 6",
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
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema));

        /* ******* String field tests start ***** */
        assertEquals(FieldType.STRING, schema.getFields().get(0).getType());
        assertEquals(schemaSummary.getId() + "-" + "f1", schema.getFields().get(0).getId());
        assertEquals("f1", schema.getFields().get(0).getName());
        assertEquals("F1", schema.getFields().get(0).getDisplayName());
        assertEquals("Test field 1", schema.getFields().get(0).getDescription());
        assertNull(schema.getFields().get(0).getParent());
        assertNull(schema.getFields().get(0).getVisibilityCondition());
        assertNull(schema.getFields().get(0).getEditableCondition());
        assertFalse(schema.getFields().get(0).isAllowMultiple());

        assertNotNull(schema.getFields().get(0).getUpdated());

        val stringFieldSchemaStored = schema.getFields().get(0);
        val stringFieldSchema = new StringFieldSchema(schema.getFields().get(0).getId(),
                                        schema.getFields().get(0).getName(),
                                        schema.getFields().get(0).getDisplayName(),
                                        schema.getFields().get(0).getDescription(),
                                        schema.getFields().get(0).getParent(),
                                        schema.getFields().get(0).getVisibilityCondition(),
                                        schema.getFields().get(0).getEditableCondition(),
                                        schema.getFields().get(0).isAllowMultiple(),
                                        new Date(),
                                        new Date(),
                                        100,
                                        null,
                                        "Default FieldValue");


        FieldSchema fieldSchema = schema.getFields().get(0);
        assertEquals(fieldSchema, schema.getFields().get(0));
        assertNotEquals(stringFieldSchemaStored, stringFieldSchema);

        assertEquals(100, stringFieldSchema.getMaxLength());
        assertNull(stringFieldSchema.getMatchPattern());
        assertEquals("Default FieldValue", stringFieldSchema.getDefaultValue());

        assertEquals(200,stringFieldSchema.setMaxLength(200).getMaxLength());
        assertEquals("Pattern", stringFieldSchema.setMatchPattern("Pattern").getMatchPattern());
        assertEquals("Default String FieldValue", stringFieldSchema.setDefaultValue("Default String FieldValue").getDefaultValue());
        /* ******* String field tests end ****** */

        /* ******* Boolean field tests start ****** */
        assertEquals(FieldType.BOOLEAN, schema.getFields().get(1).getType());
        assertEquals(schemaSummary.getId() + "-" + "f2", schema.getFields().get(1).getId());
        assertEquals("f2", schema.getFields().get(1).getName());
        assertEquals("F2", schema.getFields().get(1).getDisplayName());
        assertEquals("Test field 2", schema.getFields().get(1).getDescription());
        assertNull(schema.getFields().get(1).getParent());
        assertNull(schema.getFields().get(1).getVisibilityCondition());
        assertNull(schema.getFields().get(1).getEditableCondition());
        assertFalse(schema.getFields().get(1).isAllowMultiple());

        assertNotNull(schema.getFields().get(1).getUpdated());

        val booleanFieldSchemaStored = schema.getFields().get(1);
        val booleanFieldSchema = new BooleanFieldSchema(schema.getFields().get(1).getId(),
                schema.getFields().get(1).getName(),
                schema.getFields().get(1).getDisplayName(),
                schema.getFields().get(1).getDescription(),
                schema.getFields().get(1).getParent(),
                schema.getFields().get(1).getVisibilityCondition(),
                schema.getFields().get(1).getEditableCondition(),
                schema.getFields().get(1).isAllowMultiple(),
                new Date(),
                new Date(),
                false);

        fieldSchema = schema.getFields().get(1);
        assertEquals(fieldSchema, schema.getFields().get(1));
        assertNotEquals(booleanFieldSchemaStored, booleanFieldSchema);

        assertTrue(booleanFieldSchema.setDefaultValue(true).isDefaultValue());
        /* ****** Boolean field tests end ****** */

        /* ******* Date field tests start ****** */
        assertEquals(FieldType.DATE, schema.getFields().get(2).getType());
        assertEquals(schemaSummary.getId() + "-" + "f3", schema.getFields().get(2).getId());
        assertEquals("f3", schema.getFields().get(2).getName());
        assertEquals("F3", schema.getFields().get(2).getDisplayName());
        assertEquals("Test field 3", schema.getFields().get(2).getDescription());
        assertNull(schema.getFields().get(2).getParent());
        assertNull(schema.getFields().get(2).getVisibilityCondition());
        assertNull(schema.getFields().get(2).getEditableCondition());
        assertFalse(schema.getFields().get(2).isAllowMultiple());

        assertNotNull(schema.getFields().get(2).getUpdated());

        val dateFieldSchemaStored = schema.getFields().get(2);
        val dateFieldSchema = new DateFieldSchema(schema.getFields().get(2).getId(),
                schema.getFields().get(2).getName(),
                schema.getFields().get(2).getDisplayName(),
                schema.getFields().get(2).getDescription(),
                schema.getFields().get(2).getParent(),
                schema.getFields().get(2).getVisibilityCondition(),
                schema.getFields().get(2).getEditableCondition(),
                schema.getFields().get(2).isAllowMultiple(),
                new Date(),
                new Date(),
                new Date());

        fieldSchema = schema.getFields().get(2);
        assertEquals(fieldSchema, schema.getFields().get(2));
        assertNotEquals(dateFieldSchemaStored, dateFieldSchema);

        assertEquals(new Date(),dateFieldSchema.setDefaultValue(new Date()).getDefaultValue());
        /* ******* Date field tests end ****** */

        /* ******* Location field tests start ****** */
        assertEquals(FieldType.LOCATION, schema.getFields().get(3).getType());
        assertEquals(schemaSummary.getId() + "-" + "f4", schema.getFields().get(3).getId());
        assertEquals("f4", schema.getFields().get(3).getName());
        assertEquals("F4", schema.getFields().get(3).getDisplayName());
        assertEquals("Test field 4", schema.getFields().get(3).getDescription());
        assertNull(schema.getFields().get(3).getParent());
        assertNull(schema.getFields().get(3).getVisibilityCondition());
        assertNull(schema.getFields().get(3).getEditableCondition());
        assertFalse(schema.getFields().get(3).isAllowMultiple());

        assertNotNull(schema.getFields().get(3).getUpdated());

        val locationFieldSchemaStored = schema.getFields().get(3);
        val locationFieldSchema = new LocationFieldSchema(schema.getFields().get(3).getId(),
                schema.getFields().get(3).getName(),
                schema.getFields().get(3).getDisplayName(),
                schema.getFields().get(3).getDescription(),
                schema.getFields().get(3).getParent(),
                schema.getFields().get(3).getVisibilityCondition(),
                schema.getFields().get(3).getEditableCondition(),
                schema.getFields().get(3).isAllowMultiple(),
                new Date(),
                new Date(),
                5.0,
                -15.0
                );

        fieldSchema = schema.getFields().get(3);
        assertEquals(fieldSchema, schema.getFields().get(3));
        assertNotEquals(locationFieldSchemaStored, locationFieldSchema);

        assertEquals(10.0,locationFieldSchema.setDefaultLat(10.0).getDefaultLat());
        assertEquals(20.0,locationFieldSchema.setDefaultLon(20.0).getDefaultLon());
        /* ******* Location field tests end ****** */

        /* ******* Number field tests start ****** */
        assertEquals(FieldType.NUMBER, schema.getFields().get(4).getType());
        assertEquals(schemaSummary.getId() + "-" + "f5", schema.getFields().get(4).getId());
        assertEquals("f5", schema.getFields().get(4).getName());
        assertEquals("F5", schema.getFields().get(4).getDisplayName());
        assertEquals("Test field 5", schema.getFields().get(4).getDescription());
        assertNull(schema.getFields().get(4).getParent());
        assertNull(schema.getFields().get(4).getVisibilityCondition());
        assertNull(schema.getFields().get(4).getEditableCondition());
        assertFalse(schema.getFields().get(4).isAllowMultiple());

        assertNotNull(schema.getFields().get(4).getUpdated());

        val numberFieldSchemaStored = schema.getFields().get(4);
        val numberFieldSchema = new NumberFieldSchema(schema.getFields().get(4).getId(),
                schema.getFields().get(4).getName(),
                schema.getFields().get(4).getDisplayName(),
                schema.getFields().get(4).getDescription(),
                schema.getFields().get(4).getParent(),
                schema.getFields().get(4).getVisibilityCondition(),
                schema.getFields().get(4).getEditableCondition(),
                schema.getFields().get(4).isAllowMultiple(),
                new Date(),
                new Date(),
                100.0,
                -10.0,
                0.0
        );

        fieldSchema = schema.getFields().get(4);
        assertEquals(fieldSchema, schema.getFields().get(4));
        assertNotEquals(numberFieldSchemaStored, numberFieldSchema);

        assertEquals(200.0,numberFieldSchema.setMax(200.0).getMax());
        assertEquals(0.0,numberFieldSchema.setMin(0.0).getMin());
        assertEquals(10.0,numberFieldSchema.setDefaultValue(10.0).getDefaultValue());
        /* ******* Number field tests end ****** */

        /* ******* Choice field tests start ****** */
        assertEquals(FieldType.CHOICE, schema.getFields().get(5).getType());
        assertEquals(schemaSummary.getId() + "-" + "f6", schema.getFields().get(5).getId());
        assertEquals("f6", schema.getFields().get(5).getName());
        assertEquals("F6", schema.getFields().get(5).getDisplayName());
        assertEquals("Test field 6", schema.getFields().get(5).getDescription());
        assertNull(schema.getFields().get(5).getParent());
        assertNull(schema.getFields().get(5).getVisibilityCondition());
        assertNull(schema.getFields().get(5).getEditableCondition());
        assertFalse(schema.getFields().get(5).isAllowMultiple());
        assertEquals(2, ((ChoiceFieldSchema) schema.getFields().get(5)).getChoices().size());
        assertEquals("cat", ((ChoiceFieldSchema) schema.getFields().get(5)).getDefaultSelection());

        assertNotNull(schema.getFields().get(5).getUpdated());

        val choiceFieldSchemaStored = schema.getFields().get(5);
        val choiceFieldSchema = new ChoiceFieldSchema(schema.getFields().get(5).getId(),
                schema.getFields().get(5).getName(),
                schema.getFields().get(5).getDisplayName(),
                schema.getFields().get(5).getDescription(),
                schema.getFields().get(5).getParent(),
                schema.getFields().get(5).getVisibilityCondition(),
                schema.getFields().get(5).getEditableCondition(),
                schema.getFields().get(5).isAllowMultiple(),
                new Date(),
                new Date(),
                List.of(new ChoiceFieldSchema.Option("cat", "Cat"),
                        new ChoiceFieldSchema.Option("dog", "Dog")),
                "cat"
        );

        fieldSchema = schema.getFields().get(5);
        assertEquals(fieldSchema, schema.getFields().get(5));
        assertNotEquals(choiceFieldSchemaStored, choiceFieldSchema);

        assertEquals(List.of(
                        new ChoiceFieldSchema.Option("man", "Man"),
                        new ChoiceFieldSchema.Option("woman", "Woman")
                ),
                choiceFieldSchema.setChoices(
                        List.of(
                                new ChoiceFieldSchema.Option("man", "Man"),
                                new ChoiceFieldSchema.Option("woman", "Woman")
                        )
                ).getChoices()
        );
        assertEquals("man",choiceFieldSchema.setDefaultSelection("man").getDefaultSelection());
        /* ******* Choice field tests end ****** */

        assertEquals("Changed",
                     store.updateDescription(sId, "Changed").map(SchemaSummary::getDescription).orElse(null));
        assertEquals(SchemaState.ACTIVE,
                     store.updateState(sId, SchemaState.ACTIVE).map(SchemaSummary::getState).orElse(null));
        assertTrue(store.deleteField(sId, schema.getFields().get(0).getId()));
        assertEquals(5, store.get(sId).map(r -> r.getFields().size()).orElse(-1));
        assertEquals("Changed", store.updateField(sId, sId + "-f2", schema.getFields().get(1).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, sId + "-f3", schema.getFields().get(2).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, sId + "-f4", schema.getFields().get(3).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, sId + "-f5", schema.getFields().get(4).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertEquals("Changed", store.updateField(sId, sId + "-f6", schema.getFields().get(5).setDescription("Changed"))
                .map(FieldSchema::getDescription)
                .orElse(null));
        assertNotNull(store.getField(sId, schema.getFields().get(2).getId()).orElse(null));

        /* **** Check and confirm Scheme working ***** */

    }

    @Test
    void testFieldCreateOrUpdate(BalancedDBShardingBundle<TestConfig> bundle) {
        val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                      bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                      MAPPER);
        val schemaSummary = store.create("Test", "Test Schema").orElse(null);
        assertNotNull(schemaSummary);
        val sId = schemaSummary.getId();
        assertEquals(schemaSummary, store.getSummary(sId).orElse(null));
        assertNotNull(store.addField(sId, sId + "-f1", new StringFieldSchema("f1",
                                                                             "f1",
                                                                             "F1",
                                                                             "Test field 1",
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             false,
                                                                             new Date(),
                                                                             new Date(),
                                                                             100,
                                                                             null,
                                                                             "Default FieldValue")).orElse(null));
        {
            val fields = store.get(sId).map(Schema::getFields).orElse(List.of());
            assertEquals(1, fields.size());
            assertTrue(store.deleteField(sId, fields.get(0).getId()));
            assertTrue(store.get(sId).map(Schema::getFields).orElse(List.of()).isEmpty());
        }
        try {
            store.addField(sId, sId + "-f1", new BooleanFieldSchema("f1",
                                                                   "f1",
                                                                   "F1",
                                                                   "Test field 1",
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   false,
                                                                   new Date(),
                                                                   new Date(),
                                                                   true));
            fail("Should have failed");
        }
        catch (ConductorException e) {
            assertEquals(ConductorErrorCode.SCHEMA_FIELD_UPDATE_TYPE_MISMATCH, e.getErrorCode());
        }
        assertNotNull(store.addField(sId, sId + "-f1", new StringFieldSchema("f1",
                                                                             "f1",
                                                                             "F1",
                                                                             "Test field 1",
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             false,
                                                                             new Date(),
                                                                             new Date(),
                                                                             100,
                                                                             null,
                                                                             "Default FieldValue")).orElse(null));
        {
            val fields = store.get(sId).map(Schema::getFields).orElse(List.of());
            assertEquals(1, fields.size());
        }
    }

    @Test
    void testExceptionStoreCreateFail(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.create(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsOutOfBound(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            val schemaSummary = store.create("Test", "Test Schema").orElse(null);
            assertNotNull(schemaSummary);
            val sId = schemaSummary.getId();
            assertEquals(schemaSummary, store.getSummary(sId).orElse(null));
            val schema = store.get(sId).orElse(null);
            assertNotNull(schema);
            assertEquals(0, schema.getFields().size());
            assertEquals(FieldType.STRING, schema.getFields().get(0).getType());
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ArrayIndexOutOfBoundsException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsGetSummarySchemaNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.getSummary(null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsGetSchemaNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.get(null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }
    @Test
    void testExceptionsUpdateDescriptionNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.updateDescription(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsUpdateStateNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.updateState(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsAddFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            val fieldSchema = new StringFieldSchema("f1",
                    "f1",
                    "F1",
                    "Test field 1",
                    null,
                    null,
                    null,
                    false,
                    new Date(),
                    new Date(),
                    100,
                    null,
                    "Default FieldValue");
            store.addField(null, "f1", fieldSchema);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsGetFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.getField(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsUpdateFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.updateField(null, null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptionsDeleteFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.deleteField(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }

    @Test
    void testExceptions(BalancedDBShardingBundle<TestConfig> bundle) {

        try{
            val store = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                    MAPPER);
            store.deleteField(null, null);
            fail("Exception didn't occur - test failed");
        }
        catch(Exception e) {
            assertEquals(ConductorException.class, e.getClass());
        }
    }


}

