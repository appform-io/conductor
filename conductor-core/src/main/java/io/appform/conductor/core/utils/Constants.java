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

package io.appform.conductor.core.utils;

import lombok.experimental.UtilityClass;

/**
 *
 */
@UtilityClass
public class Constants {

    public static final String ROOT_IMPLEMENTATION_NAME = "root";
    public static final String CACHED_IMPLEMENTATION_NAME = "cached";
    public static final String BACKGROUND_JOBS_POOL_NAME = "backgroundJobsPool";

    public static final String CREATED_DATE_DEFINITION = "datetime(3) DEFAULT current_timestamp(3)";
    public static final String UPDATED_DATE_DEFINITION = "datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3)";
    public static final int MAX_DESCRIPTION_LENGTH = 512;
    public static final int MAX_SPEC_LENGTH = 5120;
    public static final int MAX_COMMENT_LENGTH = 5120;
    public static final int MAX_TEMPLATE_LENGTH = 10240;
    public static final int MAX_FIELD_NAME_LENGTH = 45;
    public static final int MAX_WORKFLOW_STATE_NAME_LENGTH = 45;
    public static final int MAX_WORKFLOW_ID_LENGTH = 45;
    public static final int MAX_CRON_LENGTH = 45;
    public static final int MAX_SESSION_ID_LENGTH = 45; //Generated ID
    public static final int MAX_ACTION_ID_LENGTH = 45; //Generated ID
    public static final int MAX_SUBJECT_GLOBAL_ID_LENGTH = 45; // Generated ID
    public static final int MAX_EXT_SUB_ID_LENGTH = 45; // Generated ID
    public static final int MAX_ADDRESS_ID_LENGTH = 45; // //Generated ID
    public static final int MAX_ATTACHMENT_ID_LENGTH = 45; //Generated ID
    public static final int MAX_COMMENT_ID_LENGTH = 45; //Generated ID
    public static final int MAX_ACTIVATION_TOKEN_LENGTH = 45; //Generated ID
    public static final int MAX_WORKFLOW_RULE_ID_LENGTH = 45; //Generated ID
    public static final int MAX_USER_ID_LENGTH = 30; //Generated ID and used in other index as composite key
    public static final int MAX_TICKET_ID_LENGTH = 30; //Generated ID and used in other index as composite key
    public static final int MAX_ROLE_ID_LENGTH = 30; //Generated ID and used in other index as composite key
    public static final int MAX_USER_ROLE_MAPPING_ID_LENGTH = MAX_ROLE_ID_LENGTH + MAX_USER_ID_LENGTH + 2 ; //Derived from userId, roleId
    public static final int MAX_GROUP_ID_LENGTH = 45; // Derived from name
    public static final int MAX_DASHBOARD_ID_LENGTH = 45; //Derived from name
    public static final int MAX_REPORT_ID_LENGTH = 45;  //Derived from name
    public static final int MAX_REPORT_RUN_ID_LENGTH = 255; //Derived from reportId, runTime, time
    public static final int MAX_SCHEMA_ID_LENGTH = 45; //Derived from name
    public static final int MAX_FIELD_ID_LENGTH = MAX_SCHEMA_ID_LENGTH + MAX_FIELD_NAME_LENGTH + 2 ; //Derived from schemaId, name
    public static final int MAX_FIELD_SCHEMA_ID_LENGTH = MAX_FIELD_ID_LENGTH + MAX_SCHEMA_ID_LENGTH  + 2; //Derived from fieldId,schemaId
    public static final int MAX_FIELD_VALUE_ID_LENGTH = MAX_TICKET_ID_LENGTH + MAX_FIELD_SCHEMA_ID_LENGTH + 2 ; //Derived from ticketId, schemaFieldId
    public static final int MAX_WORKFLOW_STATE_ID_LENGTH = MAX_WORKFLOW_ID_LENGTH + MAX_WORKFLOW_STATE_NAME_LENGTH + 2 ; //Derived from  workflowId, displayName
    public static final int MAX_WORKFLOW_TRANSITION_ID_LENGTH = MAX_WORKFLOW_ID_LENGTH + MAX_WORKFLOW_STATE_ID_LENGTH * 2 + 25; //Derived from  workflowId, from, to, time
    public static final int MAX_SKILL_ID_LENGTH = 45; //Derived from name
    public static final int MAX_SKILL_VALUE_LENGTH = 45; //Derived from name
    public static final int MAX_SKILL_VALUE_ID_LENGTH = MAX_SKILL_ID_LENGTH + MAX_SKILL_VALUE_LENGTH + 2; //Derived from skillId,value
    public static final int MAX_SKILL_ASSOCIATION_ID_LENGTH = MAX_USER_ID_LENGTH + MAX_SKILL_ID_LENGTH + MAX_SKILL_VALUE_ID_LENGTH + 3; //Derived from userId,skillId,valueId"
    public static final int MAX_INGRESS_TRANSLATOR_ID_LENGTH = 45;
    public static final int MAX_TASK_ID_LENGTH = 45; //Derived from name
    public static final int MAX_TICKET_RELATED_ID_LENGTH =  2 * MAX_TICKET_ID_LENGTH + 2; //Derived from ticketId, relatedToTicketId
    public static final int MAX_EMAIL_ID_LENGTH = 255;
    public static final int MAX_PASSWORD_LENGTH = 255;
    public static final int MAX_CQL_LENGTH = 4096;
    public static final int MAX_RECIPIENTS_LENGTH = 2048;


}
