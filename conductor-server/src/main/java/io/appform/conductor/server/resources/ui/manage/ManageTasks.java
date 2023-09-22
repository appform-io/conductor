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

package io.appform.conductor.server.resources.ui.manage;

import com.google.common.base.Strings;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterType;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketAssignedToGroup;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketPriorityIn;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketStateIn;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketWorkflowEquals;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.server.taskmanagement.TaskStore;
import io.appform.conductor.server.taskmanagement.model.*;
import io.appform.conductor.server.ui.views.tasks.RunActionOnSelectedTicketsView;
import io.appform.conductor.server.ui.views.tasks.TaskListView;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Path("/ui/manage/tasks")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class ManageTasks {
    private final WorkflowStore workflowStore;
    private final GroupStore groupStore;
    private final ActionStore actionStore;
    private final ConductorTaskScheduler scheduler;
    private final TaskStore taskStore;

    @GET
    @Path("/{workflowId}")
    public Response renderTaskList(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        val scope = Scope.build(Scope.ScopeType.WORKFLOW, workflowId);
        return render(new TaskListView(user.getUserSession().getUser(),
                                       workflowId, taskStore.listByScopes(List.of(scope)),
                                       scope));
    }

    @POST
    @Path("/{workflowId}")
    public Response renderTaskCreateScreen(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("taskType") @NotNull final TaskType taskType) {
        return switch (taskType) {
            case RUN_ACTION_ON_SELECTED_TICKETS -> {
                val workFlow = workflowStore.read(workflowId).orElse(null);
                if (null == workFlow) {
                    throw fail("No such workflow found: " + workflowId, "/manage/tasks/" + workflowId);
                }
                yield render(new RunActionOnSelectedTicketsView(user.getUserSession().getUser(),
                                                                workflowId,
                                                                workFlow.getStates().values(),
                                                                groupStore.list(),
                                                                actionStore.listActionsForScopes(List.of(Scope.GLOBAL,
                                                                                                         Scope.build(
                                                                                                                 Scope.ScopeType.WORKFLOW,
                                                                                                                 workflowId))),
                                                                null,
                                                                List.of(),
                                                                List.of(), null));
            }
            default -> redirect("/manage/tasks/" + workflowId);
        };
    }


    @GET
    @Path("/{workflowId}/{taskId}")
    public Response renderTaskDetailsScreen(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("taskId") @NotEmpty @Length(max = 45) final String taskId) {
        val workFlow = workflowStore.read(workflowId).orElse(null);
        if (null == workFlow) {
            throw fail("No such workflow found: " + workflowId, "/manage/tasks/" + workflowId);
        }
        val task = taskStore.listByIds(List.of(taskId)).stream().findFirst().orElse(null);
        if (null == task) {
            throw fail("Error getting details for task", "/manage/tasks/" + workflowId);
        }

        return switch (task.getType()) {
            case RUN_ACTION_ON_SELECTED_TICKETS -> {
                val spec = task.getSpec().accept(new TaskSpecVisitor<RunActionOnSelectedTicketsTaskSpec>() {
                    @Override
                    public RunActionOnSelectedTicketsTaskSpec visit(RunActionOnSelectedTicketsTaskSpec runActionOnSelectedTicketsTaskSpec) {
                        return runActionOnSelectedTicketsTaskSpec;
                    }
                });
                yield render(new RunActionOnSelectedTicketsView(user.getUserSession().getUser(),
                                                                workflowId,
                                                                workFlow.getStates().values(),
                                                                groupStore.list(),
                                                                actionStore.listActionsForScopes(List.of(Scope.GLOBAL,
                                                                                                         Scope.build(
                                                                                                                 Scope.ScopeType.WORKFLOW,
                                                                                                                 workflowId))),
                                                                task,
                                                                spec.getTicketFilters()
                                                                        .stream()
                                                                        .filter(tf -> tf.getType().equals(
                                                                                TicketFilterType.STATE_IN))
                                                                        .map(TicketStateIn.class::cast)
                                                                        .flatMap(tsi -> tsi.getStateIds().stream())
                                                                        .toList(),
                                                                spec.getTicketFilters()
                                                                        .stream()
                                                                        .filter(tf -> tf.getType().equals(
                                                                                TicketFilterType.ASSIGNED_TO_GROUP))
                                                                        .map(TicketAssignedToGroup.class::cast)
                                                                        .flatMap(tag -> tag.getAssignedGroupIds()
                                                                                .stream())
                                                                        .toList(),
                                                                spec.getTicketFilters()
                                                                        .stream()
                                                                        .filter(tf -> tf.getType().equals(
                                                                                TicketFilterType.PRIORITY_IN))
                                                                        .map(TicketPriorityIn.class::cast)
                                                                        .flatMap(tag -> tag.getPriorities().stream())
                                                                        .toList()));
            }
            default -> throw fail("Unsupported", "/manage/tasks/" + workflowId);
        };
    }

    @POST
    @Path("/{workflowId}/{taskId}/pause")
    public Response pauseTask(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("taskId") @NotEmpty @Length(max = 45) final String taskId) {
        return taskStore.update(taskId, task -> task.withState(TaskState.PAUSED))
                .map(task -> redirect("/manage/tasks/" + workflowId))
                .orElseThrow(() -> fail("Could not update task " + taskId,
                                        "/manage/tasks/" + workflowId + "/" + taskId));
    }

    @POST
    @Path("/{workflowId}/{taskId}/activate")
    public Response activateTask(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("taskId") @NotEmpty @Length(max = 45) final String taskId) {
        return taskStore.update(taskId, task -> task.withState(TaskState.ACTIVE))
                .map(task -> redirect("/manage/tasks/" + workflowId))
                .orElseThrow(() -> fail("Could not update task " + taskId,
                                        "/manage/tasks/" + workflowId + "/" + taskId));
    }

    @POST
    @Path("/{workflowId}/{taskId}/delete")
    public Response deleteTask(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("taskId") @NotEmpty @Length(max = 45) final String taskId) {
        if (taskStore.delete(taskId)) {
            return redirect("/manage/tasks/" + workflowId);
        }
        throw fail("Could not update task " + taskId, "/manage/tasks/" + workflowId + "/" + taskId);
    }


    @POST
    @Path("/{workflowId}/RUN_ACTION_ON_SELECTED_TICKETS")
    public Response createRunOnSelectedTicketsTask(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @NotEmpty @Length(max = 255) final String description,
            @FormParam("interval") @Min(0) final int interval,
            @FormParam("stateIds") Set<String> stateIds,
            @FormParam("groupIds") Set<String> groupIds,
            @FormParam("priorities") Set<TicketPriority> priorities,
            @FormParam("selectedActions") List<String> actionIds) {
        val workFlow = workflowStore.read(workflowId).orElse(null);
        if (null == workFlow) {
            throw fail("No such workflow found: " + workflowId, "/manage/tasks/" + workflowId);
        }
        val spec = buildSpec(workflowId, stateIds, groupIds, priorities, actionIds);
        val task = Task.builder()
                .name(name)
                .description(description)
                .interval(Duration.ofMinutes(interval))
                .type(TaskType.RUN_ACTION_ON_SELECTED_TICKETS)
                .scope(Scope.build(Scope.ScopeType.WORKFLOW, workflowId))
                .state(TaskState.ACTIVE)
                .spec(spec)
                .build();
        val taskId = scheduler.scheduleNewTask(task);
        if (Strings.isNullOrEmpty(taskId)) {
            throw fail("Unable to save task", "/manage/tasks/" + workflowId);
        }
        return redirect("/manage/tasks/" + workflowId + "/" + taskId);
    }

    @POST
    @Path("/{workflowId}/RUN_ACTION_ON_SELECTED_TICKETS/{taskId}/update")
    public Response updateRunOnSelectedTicketsTask(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("taskId") @NotEmpty @Length(max = 45) final String taskId,
            @FormParam("description") @NotEmpty @Length(max = 255) final String description,
            @FormParam("interval") @Min(0) final int interval,
            @FormParam("stateIds") Set<String> stateIds,
            @FormParam("groupIds") Set<String> groupIds,
            @FormParam("priorities") Set<TicketPriority> priorities,
            @FormParam("selectedActions") List<String> actionIds) {
        val workFlow = workflowStore.read(workflowId).orElse(null);
        if (null == workFlow) {
            throw fail("No such workflow found: " + workflowId, "/manage/tasks/" + workflowId);
        }
        val spec = buildSpec(workflowId, stateIds, groupIds, priorities, actionIds);
        val updated = scheduler.updateTask(taskId,
                                           task -> task.withDescription(description)
                                                   .withInterval(Duration.ofMinutes(interval))
                                                   .withSpec(spec));
        if (!updated) {
            throw fail("Unable to save task " + taskId, "/manage/tasks/" + workflowId);
        }
        return redirect("/manage/tasks/" + workflowId + "/" + taskId);
    }

    private static RunActionOnSelectedTicketsTaskSpec buildSpec(
            String workflowId,
            Set<String> stateIds,
            Set<String> groupIds,
            Set<TicketPriority> priorities,
            List<String> actionIds) {
        final var specBuilder = RunActionOnSelectedTicketsTaskSpec.builder();
        val tfs = new ArrayList<TicketFilter>();
        tfs.add(new TicketWorkflowEquals(workflowId));
        if (null != stateIds && !stateIds.isEmpty()) {
            tfs.add(new TicketStateIn(stateIds, false));
        }
        if (null != groupIds && !groupIds.isEmpty()) {
            tfs.add(new TicketAssignedToGroup(groupIds));
        }
        if (null != priorities && !priorities.isEmpty()) {
            tfs.add(new TicketPriorityIn(priorities));
        }
        val spec = specBuilder.ticketFilters(tfs)
                .actionIds(actionIds)
                .build();
        return spec;
    }
}
