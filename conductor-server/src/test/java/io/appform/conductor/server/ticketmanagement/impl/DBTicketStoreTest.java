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

package io.appform.conductor.server.ticketmanagement.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.schema.fields.NumberFieldSchema;
import io.appform.conductor.model.schema.fields.StringFieldSchema;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.ColumnGroupingElement;
import io.appform.conductor.model.ticket.fields.impl.BooleanFieldValue;
import io.appform.conductor.model.ticket.fields.impl.NumberFieldValue;
import io.appform.conductor.model.ticket.fields.impl.StringFieldValue;
import io.appform.conductor.model.ticket.filter.fieldfilters.TicketFieldEquals;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredRelatedTicket;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static io.appform.conductor.server.utils.ConductorServerUtils.configureMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.ticketmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBTicketStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setup() {
        configureMapper(MAPPER);
    }

    @Test
    void testCRUD(final BalancedDBShardingBundle<TestConfig> bundle) {
        val store = new DBTicketStore(bundle.createParentObjectDao(StoredTicketSkeleton.class),
                                      bundle.createRelatedObjectDao(StoredFieldValue.class),
                                      bundle.createRelatedObjectDao(StoredComment.class),
                                      bundle.createRelatedObjectDao(StoredAttachment.class),
                                      bundle.createRelatedObjectDao(StoredRelatedTicket.class),
                                      MAPPER);
        val created = store.create("T001",
                                   "Test",
                                   "This is a test ticket",
                                   "WF001",
                                   "S001",
                                   "TS001",
                                   TicketPriority.MEDIUM,
                                   null,
                                   List.of(new TicketFieldData("TF001", new BooleanFieldValue(true)),
                                           new TicketFieldData("TF002", new StringFieldValue("Random")),
                                           new TicketFieldData("TF004", new StringFieldValue("Random Value"))))
                .orElse(null);

        assertNotNull(store.create("T002",
                                   "Test",
                                   "This is a test ticket",
                                   "WF001",
                                   "S001",
                                   "TS001",
                                   TicketPriority.MEDIUM,
                                    null,
                                    List.of(new TicketFieldData("TF001", new BooleanFieldValue(true)),
                                           new TicketFieldData("TF003", new NumberFieldValue(23)),
                                           new TicketFieldData("TF004", new StringFieldValue("Random Value"))))
                              .orElse(null));
        assertNotNull(store.create("T003",
                                   "Test",
                                   "This is a test ticket",
                                   "WF001",
                                   "S001",
                                   "TS001",
                                   TicketPriority.MEDIUM,
                                    null,
                                    List.of(new TicketFieldData("TF001", new BooleanFieldValue(true)),
                                           new TicketFieldData("TF003", new NumberFieldValue(23)),
                                           new TicketFieldData("TF004", new StringFieldValue("Random Value"))))
                              .orElse(null));
        assertNotNull(store.create("T009",
                                   "Test",
                                   "This is a test ticket",
                                   "WF001",
                                   "S001",
                                   "TS001",
                                   TicketPriority.MEDIUM,
                                    null,
                                   List.of())
                              .orElse(null));
        assertNotNull(created);
        val read = store.read("T001", true).orElse(null);
        assertNotNull(read);
        assertEquals(3, read.getFields().size());

        val updated = store.update("T001",
                                   ticketSkeleton ->
                                           ticketSkeleton.setTitle("Test Updated")
                                                   .setDescription("This is a test ticket update")
                                                   .setSubjectId("S001")
                                                   .setTicketStateId("TS001")
                                                   .setPriority(TicketPriority.HIGH),
                                   List.of(new TicketFieldData("TF003", new NumberFieldValue(23)),
                                           new TicketFieldData("TF002", new StringFieldValue("Random updated value"))))
                .orElse(null);
        assertNotNull(updated);
        assertEquals(4, updated.getFields().size());

        val relevantFieldSchema = Map.of("TF003", new NumberFieldSchema("S1",
                                                                        "TF003",
                                                                        "Age",
                                                                        "",
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        100,
                                                                        0,
                                                                        0),
                                         "TF002", new StringFieldSchema("S1",
                                                                        "TF002",
                                                                        "Name",
                                                                        "",
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        100,
                                                                        null,
                                                                        null),
                                         "TF004", new StringFieldSchema("S1",
                                                                        "TF004",
                                                                        "City",
                                                                        "",
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        100,
                                                                        null,
                                                                        null));
        var list = store.older(
                List.of(), List.of(), null,
                Integer.MAX_VALUE, relevantFieldSchema, false, List.of());
        assertEquals(4, list.getResults().size());
        list = store.older(List.of(),
                           List.of(new TicketFieldEquals("TF003", 23.0),
                                  new TicketFieldEquals("TF004", "Random Value")
                                 ), null,
                           Integer.MAX_VALUE, relevantFieldSchema, true, List.of("TF004"));
        assertEquals(3, list.getResults().size());
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        list = store.older(List.of(),
                           List.of(new TicketFieldEquals("TF003", 23.0),
                                  new TicketFieldEquals("TF002", "Random updated value"),
                                  new TicketFieldEquals("TF004", "Random Value")
                                 ), null,
                           Integer.MAX_VALUE, relevantFieldSchema, false, List.of());
        assertEquals(1, list.getResults().size());
        assertNotNull(store.create("T004",
                                   "Test",
                                   "This is a test ticket",
                                   "WF001",
                                   "S001",
                                   "TS002",
                                   TicketPriority.MEDIUM,
                                   null,
                                   List.of(new TicketFieldData("TF001", new BooleanFieldValue(true)),
                                           new TicketFieldData("TF003", new NumberFieldValue(23)),
                                           new TicketFieldData("TF004", new StringFieldValue("Random Value"))))
                              .orElse(null));
        val groups = store.groupCount("r1",
                                      List.of(),
                                      List.of(new TicketFieldEquals("TF003", 23.0)),
                                      relevantFieldSchema,
                                      List.of(new ColumnGroupingElement(StoredTicketSkeleton.Fields.priority,null),
                                              new ColumnGroupingElement(StoredTicketSkeleton.Fields.ticketStateId, null)));
        /*assertEquals(2, groups.getCounts().rowKeySet().size());
        assertEquals(1, groups.getCounts().columnMap().get("HIGH").getCounts().size());
        assertEquals(1, groups.getCounts().getChildren().get("HIGH").getCounts().get("TS001"));
        assertEquals(2, groups.getCounts().getChildren().get("MEDIUM").getCounts().size());
        assertEquals(2, groups.getCounts().getChildren().get("MEDIUM").getCounts().get("TS001"));
        assertEquals(1, groups.getCounts().getChildren().get("MEDIUM").getCounts().get("TS002"));

        {
            val ts = store.timeSeries(null, List.of(), List.of(),
                                      "ticketStateId",
                                      TimeResolution.MINUTE,
                                      relevantFieldSchema
                                     );
            assertNotNull(ts);
            assertEquals(2, ts.getSeries().size());
            assertEquals(3, ts.getSeries().get("TS001").values().stream().mapToLong(i -> i).sum());
            assertEquals(1, ts.getSeries().get("TS002").values().stream().mapToLong(i -> i).sum());
        }
        {
            val ts = store.timeSeries(null, List.of(), List.of(),
                                      null,
                                      TimeResolution.MINUTE,
                                      relevantFieldSchema
                                     );
            assertNotNull(ts);
            assertEquals(1, ts.getSeries().size());
            assertEquals(4, ts.getSeries().get(TicketTimeSeriesResponse.DEFAULT_FIELD).values().stream().mapToLong(i
            -> i).sum());
        }*/

    }
}