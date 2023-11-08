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
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.EventGeneratingActionStore;
import io.appform.conductor.server.actionmanagement.impl.DBActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.conductor.server.auth.EventGeneratingRoleStore;
import io.appform.conductor.server.auth.EventGeneratingUserRoleMappingStore;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.auth.impl.DBRoleStore;
import io.appform.conductor.server.auth.impl.DBUserRoleMappingStore;
import io.appform.conductor.server.auth.impl.models.StoredRole;
import io.appform.conductor.server.auth.impl.models.StoredUserRoleMapping;
import io.appform.conductor.server.config.AppConfig;
import io.appform.conductor.server.config.AuthConfig;
import io.appform.conductor.server.config.MailConfig;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.EventHandler;
import io.appform.conductor.server.eventmanagement.EventHandlerImplementation;
import io.appform.conductor.server.eventmanagement.impl.SignalDrivenEventBus;
import io.appform.conductor.server.reporting.ReportStore;
import io.appform.conductor.server.reporting.impl.DBReportStore;
import io.appform.conductor.server.reporting.impl.models.StoredReport;
import io.appform.conductor.server.reporting.impl.models.StoredReportContext;
import io.appform.conductor.server.reporting.impl.models.StoredReportRun;
import io.appform.conductor.server.schemamanagement.impl.DBSchemaStore;
import io.appform.conductor.server.schemamanagement.impl.EventGeneratingSchemaStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.conductor.server.skillmanagement.EventGeneratingSkillStore;
import io.appform.conductor.server.skillmanagement.SkillStore;
import io.appform.conductor.server.skillmanagement.impl.DBSkillStore;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillDefinition;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillValue;
import io.appform.conductor.server.skillmanagement.impl.models.StoredUserSkillAssociation;
import io.appform.conductor.server.subjectmanagement.EventGeneratingSubjectStore;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.DBSubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredAddress;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectID;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectSummary;
import io.appform.conductor.server.taskmanagement.TaskStore;
import io.appform.conductor.server.taskmanagement.impl.DBTaskStore;
import io.appform.conductor.server.taskmanagement.impl.models.StoredTask;
import io.appform.conductor.server.ticketmanagement.EventGeneratingTicketStore;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.ticketmanagement.impl.DBTicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.ticketmanagement.impl.models.references.StoredTicketReferenceID;
import io.appform.conductor.server.usermanagement.*;
import io.appform.conductor.server.usermanagement.impl.*;
import io.appform.conductor.server.usermanagement.impl.models.*;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import io.appform.conductor.server.workflowmanagement.EventGeneratingWorkflowStore;
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
import lombok.val;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.reflections.Reflections;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.concurrent.ExecutorService;

import static org.reflections.Reflections.log;

/**
 * Guice module
 */
@RequiredArgsConstructor
@IgnoreGenerated
@SuppressWarnings("unused")
public class ConductorModule extends AbstractModule {
    public static final String ROOT_IMPLEMENTATION_NAME = "root";
    public static final String BACKGROUND_JOBS_POOL_NAME = "backgroundJobsPool";

    private final BalancedDBShardingBundle<AppConfig> dbBundle;
    private final Reflections reflections = new Reflections("io.appform.conductor");

    @Override
    protected void configure() {
        bind(UserStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBUserStore.class);
        bind(UserStore.class).to(EventGeneratingUserStore.class);

        bind(GroupStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBGroupStore.class);
        bind(GroupStore.class).to(EventGeneratingGroupStore.class);

        bind(SessionStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBSessionStore.class);
        bind(SessionStore.class).to(EventGeneratingSessionStore.class);

        bind(UserActivationTokenStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBUserActivationTokenStore.class);
        bind(UserActivationTokenStore.class).to(EventGeneratingUserActivationTokenStore.class);

        bind(UserPasswordAuthStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBUserPasswordAuthStore.class);
        bind(UserPasswordAuthStore.class).to(EventGeneratingUserPasswordAuthStore.class);

        bind(RoleStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBRoleStore.class);
        bind(RoleStore.class).to(EventGeneratingRoleStore.class);

        bind(UserRoleMappingStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBUserRoleMappingStore.class);
        bind(UserRoleMappingStore.class).to(EventGeneratingUserRoleMappingStore.class);

        bind(SkillStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBSkillStore.class);
        bind(SkillStore.class).to(EventGeneratingSkillStore.class);

        bind(SubjectStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBSubjectStore.class);
        bind(SubjectStore.class).to(EventGeneratingSubjectStore.class);

        bind(ActionStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBActionStore.class);
        bind(ActionStore.class).to(EventGeneratingActionStore.class);

        bind(SchemaStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBSchemaStore.class);
        bind(SchemaStore.class).to(EventGeneratingSchemaStore.class);

        bind(WorkflowStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBWorkflowStore.class);
        bind(WorkflowStore.class).to(EventGeneratingWorkflowStore.class);

        bind(TicketStore.class).annotatedWith(Names.named(ROOT_IMPLEMENTATION_NAME)).to(DBTicketStore.class);
        bind(TicketStore.class).to(EventGeneratingTicketStore.class);

        bind(TaskStore.class).to(DBTaskStore.class); //TODO::EVENTS
        bind(ReportStore.class).to(DBReportStore.class); //TODO::EVENTS

    }

    @Provides
    @Singleton
    @Named("eventHandlingPool")
    public ExecutorService executorService(final Environment environment) {
        return environment.lifecycle().executorService("event-handler").build();
    }

    @Provides
    @Singleton
    @Named(BACKGROUND_JOBS_POOL_NAME)
    public ExecutorService backgroundJobsPool(final Environment environment) {
        return environment.lifecycle().executorService("background-jobs").build();
    }

    @Provides
    @Singleton
    public AuthConfig authConfig(final AppConfig appConfig) {
        return appConfig.getAuth();
    }

    @Provides
    @Singleton
    public MailConfig mailConfig(final AppConfig appConfig) {
        return appConfig.getMailConfig();
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
    public RelationalDao<StoredTicketReferenceID> ticketReferenceIDDao() {
        return dbBundle.createRelatedObjectDao(StoredTicketReferenceID.class);
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

    @Provides
    @Singleton
    public LookupDao<StoredSkillDefinition> skillDao() {
        return dbBundle.createParentObjectDao(StoredSkillDefinition.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredSkillValue> skillValueDao() {
        return dbBundle.createRelatedObjectDao(StoredSkillValue.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredUserSkillAssociation> skillAssociationDao() {
        return dbBundle.createRelatedObjectDao(StoredUserSkillAssociation.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredTask> taskDao() {
        return dbBundle.createParentObjectDao(StoredTask.class);
    }

    @Provides
    @Singleton
    public LookupDao<StoredReport> reportDao() {
        return dbBundle.createParentObjectDao(StoredReport.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredReportRun> reportRunDao() {
        return dbBundle.createRelatedObjectDao(StoredReportRun.class);
    }

    @Provides
    @Singleton
    public RelationalDao<StoredReportContext> reportContextDao() {
        return dbBundle.createRelatedObjectDao(StoredReportContext.class);
    }

    @Provides
    @Singleton
    public CloseableHttpClient httpClient() {
        return ConductorServerUtils.createHttpClient();
    }

    @Provides
    @Singleton
    public EventBus eventBus(Injector injector) {
        val handlers = reflections.getTypesAnnotatedWith(EventHandlerImplementation.class);
        val eventBus = injector.getInstance(SignalDrivenEventBus.class);
        handlers.forEach(handlerClass -> {
            val handler = (EventHandler)injector.getInstance(handlerClass);
            eventBus.register(handler);
            log.info("Registered event handler: {}", handlerClass.getSimpleName());
        });
        return eventBus;
    }
}
