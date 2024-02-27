package io.appform.conductor.model.workflow;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.tasks.Task;
import lombok.Value;

import java.util.List;

/**
 * Detailed workflow information
 */
@Value
public class WorkflowDetails {

    Workflow workflow;

    Schema schema;

    List<Action> actions;

    List<Task> tasks;
}
