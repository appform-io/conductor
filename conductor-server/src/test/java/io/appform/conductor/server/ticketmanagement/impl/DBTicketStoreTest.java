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

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.impl.BooleanFieldValue;
import io.appform.conductor.model.ticket.fields.impl.NumberFieldValue;
import io.appform.conductor.model.ticket.fields.impl.StringFieldValue;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredTicketFieldValue;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import lombok.val;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.ticketmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBTicketStoreTest {

    @Test
    void testCRUD(final BalancedDBShardingBundle<TestConfig> bundle) {
        val store = new DBTicketStore(bundle.createParentObjectDao(StoredTicketSkeleton.class),
                                      bundle.createRelatedObjectDao(StoredTicketFieldValue.class));
        val created = store.create("T001",
                     "Test",
                     "This is a test ticket",
                     "WF001",
                     "S001",
                     "TS001",
                     TicketPriority.MEDIUM,
                     List.of(new TicketFieldData("TF001", new BooleanFieldValue(true)),
                             new TicketFieldData("TF002", new StringFieldValue("Random"))))
                .orElse(null);
        assertNotNull(created);
        val read = store.read("T001", true).orElse(null);
        assertNotNull(read);
        assertEquals(2, read.getFields().size());

        val updated = store.update("T001",
                                  "Test Updated",
                                  "This is a test ticket update",
                                  "S001",
                                  "TS001",
                                  TicketPriority.MEDIUM,
                                  List.of(new TicketFieldData("TF003", new NumberFieldValue(23)),
                                          new TicketFieldData("TF002", new StringFieldValue("Random updated value"))))
                .orElse(null);
        assertNotNull(updated);
        assertEquals(3, updated.getFields().size());
    }
}