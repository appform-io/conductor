/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.error;

import lombok.Getter;

/**
 * Global error table
 */
@Getter
public enum ConductorErrorCode {
    SUCCESS(0000, "Successful"),

    STORE_READ_ERROR(1001, "Error reading database for type ${type} with ${id}"),
    STORE_WRITE_ERROR(1002, "Error saving to database for type ${type} with ${id}"),
    STORE_UPDATE_ERROR(1003, "Error updating database for type ${type} with id ${id}"),
    STORE_LIST_ERROR(1004, "Error reading object list of type ${type}"),
    STORE_QUERY_ERROR(1005, "Error reading object of type ${type} using query of param type ${paramType} and value ${value}"),
    STORE_RELATED_ENTITY_READ_ERROR(1006, "Error reading database for type ${type} with ${id}/${subId}"),
    STORE_RELATED_ENTITY_WRITE_ERROR(1007, "Error saving to database for type ${type} with ${id}/${subId}"),
    STORE_RELATED_ENTITY_UPDATE_ERROR(1008, "Error updating database for type ${type} with id ${id}/${subId}"),
    STORE_RELATED_ENTITY_LIST_ERROR(1009, "Error listing related entities of type ${type} with parent id ${id}"),


    SCHEMA_UPDATE_FAILED(2001, "Error updating schema version ${schemaId}/${version}. Operation attempted: ${operation}"),
    SCHEMA_FIELD_WRITE_FAILED(2002, "Error writing schema field for ${schemaId}/${fieldId}"),
    SCHEMA_FIELD_READ_FAILED(2003, "Error reading schema field for ${schemaId}/${fieldId}"),
    SCHEMA_FIELD_UPDATE_TYPE_MISMATCH(2004, "Type mismatch for ${schemaId}/${fieldId}. Old: ${oldType} New: ${newType}"),

    WORKFLOW_ERROR_INVALID_ID(3001, "Invalid workflow ID: ${id}"),
    WORKFLOW_ERROR(3002, "Error in workflow management: ${message}"),
    WORKFLOW_ERROR_INVALID_INITIAL_STATE(3003, "Error setting initial workflow: ${message}"),

    TICKET_MGMT_NO_WORKFLOW(4001, "No workflow found for given payload"),
    TICKET_MGMT_NO_SCHEMA(4002, "No active schema found for ${workflowId}/${schemaId}"),
    TICKET_SCHEMA_VALIDATION_FAILURE(4003, "There are schema validation failures."),
    TICKET_SUBJECT_ID_EXTRACTION_FAILURE(4004, "Could not extract subject id from payload."),
    TICKET_NO_TRANSITION(4005, "Could not find any transition for ticket ${ticketId}." +
            " Workflow: ${workflow}. Current state: ${state}"),
    TICKET_MGMT_NO_ACTION(4006, "No action found for ${ticketId}/${workflowId}/${actionId}"),
    TICKET_MGMT_NO_TICKET(4007, "No ticket found for ${ticketId}"),
    TICKET_MGMT_NO_SUBJECT(4008, "No subject found for ${subjectId}"),
    TICKET_MGMT_NO_STATE_ACTION(4009, "No action found for ${ticketId}/${stateId}/${actionId}"),
    TICKET_MGMT_MISSING_FIELDS(4010, "Missing fields for ${ticketId} in state ${state}"),
    TICKET_MGMT_NON_EDITABLE_FIELDS_UPDATED(4011, "Updating non editable fields in state ${state}"),



    DATA_FORMAT_ERROR(5001, "Error reading data. Format error. Conversion to: ${type}. Content: ${content}"),
    INVALID_RULE_TYPE(5002, "Unsupported rule type ${ruleType}"),
    INVALID_TEMPLATE_TYPE(5003, "Unsupported template type ${templateType}"),

    CQL_PARSING_ERROR(6001, "Error parsing CQL. Errors: ${cqlError}"),
    ;

    private final int errorCode;
    private final String messageFormat;

    ConductorErrorCode(int errorCode, String messageFormat) {
        this.errorCode = errorCode;
        this.messageFormat = messageFormat;
    }
}
