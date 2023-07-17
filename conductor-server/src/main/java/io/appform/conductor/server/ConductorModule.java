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

package io.appform.conductor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.impl.DBActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.auth.impl.DBRoleStore;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.auth.impl.DBUserRoleMappingStore;
import io.appform.conductor.server.auth.impl.models.StoredRole;
import io.appform.conductor.server.auth.impl.models.StoredUserRoleMapping;
import io.appform.conductor.server.config.AppConfig;
import io.appform.conductor.server.config.AuthConfig;
import io.appform.conductor.server.schemamanagement.impl.DBSchemaStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.DBSubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredAddress;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectID;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectSummary;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.ticketmanagement.impl.DBTicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.usermanagement.*;
import io.appform.conductor.server.usermanagement.impl.*;
import io.appform.conductor.server.usermanagement.impl.models.*;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.appform.conductor.server.workflowmanagement.impl.DBWorkflowStore;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.dropwizard.setup.Environment;
import lombok.RequiredArgsConstructor;

/**
 * Guice module
 */
@RequiredArgsConstructor
@IgnoreGenerated
@SuppressWarnings("unused")
public class ConductorModule extends AbstractModule {
    private final BalancedDBShardingBundle<AppConfig> dbBundle;

    @Override
    protected void configure() {
        bind(UserStore.class).to(DBUserStore.class);
        bind(GroupStore.class).to(DBGroupStore.class);
        bind(SessionStore.class).to(DBSessionStore.class);
        bind(UserActivationTokenStore.class).to(DBUserActivationTokenStore.class);
        bind(UserPasswordAuthStore.class).to(DBUserPasswordAuthStore.class);

        bind(RoleStore.class).to(DBRoleStore.class);
        bind(UserRoleMappingStore.class).to(DBUserRoleMappingStore.class);

        bind(SubjectStore.class).to(DBSubjectStore.class);
        bind(ActionStore.class).to(DBActionStore.class);
        bind(SchemaStore.class).to(DBSchemaStore.class);
        bind(WorkflowStore.class).to(DBWorkflowStore.class);
        bind(TicketStore.class).to(DBTicketStore.class);
    }

    @Provides
    @Singleton
    public AuthConfig authConfig(final AppConfig appConfig) {
        return appConfig.getAuth();
    }

    @Provides
    @Singleton
    public LookupDao<StoredUser> storedUserLookupDao() {
        return dbBundle.createParentObjectDao(StoredUser.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredGroup> groupDao() {
        return dbBundle.createParentObjectDao(StoredGroup.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredGroupUserMapping> groupUsersDao() {
        return dbBundle.createRelatedObjectDao(StoredGroupUserMapping.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredUserSessionDetails> sessionDetailsDao() {
        return dbBundle.createRelatedObjectDao(StoredUserSessionDetails.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredUserActivationToken> tokenDao() {
        return dbBundle.createParentObjectDao(StoredUserActivationToken.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredUserPassword> passwordDao() {
        return dbBundle.createRelatedObjectDao(StoredUserPassword.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredRole> roleDao() {
        return dbBundle.createParentObjectDao(StoredRole.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredUserRoleMapping> userRoleDao() {
        return dbBundle.createRelatedObjectDao(StoredUserRoleMapping.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredSubjectSummary> subjectDao() {
        return dbBundle.createParentObjectDao(StoredSubjectSummary.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredSubjectID> subjectIdDao() {
        return dbBundle.createRelatedObjectDao(StoredSubjectID.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredAddress> addressDao() {
        return dbBundle.createRelatedObjectDao(StoredAddress.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredAction> actionDao() {
        return dbBundle.createParentObjectDao(StoredAction.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredSchemaSummary> schemaDao() {
        return dbBundle.createParentObjectDao(StoredSchemaSummary.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredFieldSchema> fieldDao() {
        return dbBundle.createRelatedObjectDao(StoredFieldSchema.class);
    }

    @Provides
    @Singleton
    public ObjectMapper mapper(final Environment environment) {
        return environment.getObjectMapper();
    }

    @Provides
    @Singleton
    public LookupDao<StoredWorkflow> wfDao() {
        return dbBundle.createParentObjectDao(StoredWorkflow.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredTicketState> tsDao() {
        return dbBundle.createRelatedObjectDao(StoredTicketState.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredTicketStateTransition> tstrnDao() {
        return dbBundle.createRelatedObjectDao(StoredTicketStateTransition.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredWorkflowSelectionRule> wfselDao() {
        return dbBundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredTicketSkeleton> ticketDao() {
        return dbBundle.createParentObjectDao(StoredTicketSkeleton.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredFieldValue> ticketFieldDao() {
        return dbBundle.createRelatedObjectDao(StoredFieldValue.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredComment> commentD() {
        return dbBundle.createRelatedObjectDao(StoredComment.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredAttachment> attachmentDao() {
        return dbBundle.createRelatedObjectDao(StoredAttachment.class);
    }
}
