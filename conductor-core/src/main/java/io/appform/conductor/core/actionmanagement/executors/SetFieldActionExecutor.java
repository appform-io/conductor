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

package io.appform.conductor.server.actionmanagement.executors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.SetFieldAction;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldSchemaVisitor;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.TicketFieldMapper;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Executes a {@link SetFieldAction}
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class SetFieldActionExecutor {
    private final TicketStore ticketStore;
    private final TemplateEngine templateEngine;
    private final TicketFieldMapper ticketFieldMapper;
    private final ObjectMapper mapper;

    public ActionExecutionResult run(SetFieldAction action, final ActionExecutor.ActionEvalData evalData) {
        val ticketId = evalData.getTicket().getSummary().getId();
        val schema = evalData.getSchema();
        val evalDataJson = ConductorServerUtils.evalDataJson(mapper, evalData);
        val fieldSchema = schema.getFields().stream()
                .filter(field -> field.getName().equals(action.getFieldSchemaName()))
                .findFirst()
                .orElse(null);

        if (fieldSchema == null) {
            log.error("No such field {} for ticket {}", action.getFieldSchemaName(), ticketId);
            return ActionExecutionResult.FAILURE;
        }

        log.info("Action:{}, Eval Data:{}", action.getId(), evalDataJson);
        String fieldValueString = templateEngine.evaluateToText(action.getFieldValueTemplate(), evalDataJson).orElse(null);
        if (fieldValueString == null) {
            log.error("No data post translation field {} for ticket {}", fieldSchema.getId(), ticketId);
            return ActionExecutionResult.FAILURE;
        }

        val fieldValue = toFieldValue(fieldSchema, fieldValueString);
        if (fieldValue == null) {
            log.error("No value not valid post translation field {} for ticket {}", fieldSchema.getId(), ticketId);
            return ActionExecutionResult.FAILURE;
        }

        if (ticketStore.setField(ticketId,
                        new TicketFieldData(fieldSchema.getId(), fieldValue))
                            .filter(ticket -> ticket.getFields()
                            .stream()
                            .anyMatch(field -> field.getFieldSchemaId().equals(fieldSchema.getId())))
                .isEmpty()) {
            log.error("Failed to add field {} to ticket {}", fieldSchema.getId(), ticketId);
            return ActionExecutionResult.FAILURE;
        }
        return ActionExecutionResult.SUCCESS;
    }

    private FieldValue toFieldValue(FieldSchema fieldSchema, String fieldValueString) {
        val fieldValueJson = toJsonNode(fieldSchema, fieldValueString);
        val results = ticketFieldMapper.validateField(fieldSchema, fieldSchema.getName(), fieldValueJson);
        if (!results.getFirst().isEmpty()) {
            log.error("Field value not valid post translation field {} fieldValue {}", fieldSchema.getId(),
                    fieldValueString);
            return null;
        }
        return results.getSecond();
    }

    private JsonNode toJsonNode(FieldSchema fieldSchema, String fieldValueString) {
        return fieldSchema.accept(new FieldSchemaVisitor<>() {
            @Override
            public JsonNode visit(StringFieldSchema stringField) {
                return mapper.createObjectNode().textNode(fieldValueString);
            }

            @Override
            public JsonNode visit(NumberFieldSchema numberField) {
                return mapper.createObjectNode().numberNode(Double.parseDouble(fieldValueString));
            }

            @Override
            public JsonNode visit(BooleanFieldSchema booleanField) {
                return mapper.createObjectNode().booleanNode(Boolean.parseBoolean(fieldValueString));
            }

            @SneakyThrows
            @Override
            public JsonNode visit(LocationFieldSchema locationField) {
               val map = mapper.readValue(fieldValueString, new TypeReference<>() {
                });
                return mapper.valueToTree(map);
            }

            @Override
            public JsonNode visit(DateFieldSchema dateField) {
                return mapper.createObjectNode().numberNode(Long.parseLong(fieldValueString));
            }

            @Override
            public JsonNode visit(ChoiceFieldSchema choiceField) {
                return mapper.valueToTree(fieldValueString.split(","));
            }
        });
    }
}
