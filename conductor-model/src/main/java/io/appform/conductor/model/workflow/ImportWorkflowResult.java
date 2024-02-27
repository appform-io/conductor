package io.appform.conductor.model.workflow;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.tasks.Task;
import lombok.Value;

import java.util.List;

@Value
public class ImportWorkflowResult {

     ImportResult<Workflow> workflow;

     ImportResult<Schema> schema;

     List<ImportResult<Action>> actions;

     List<ImportResult<Task>> tasks;
}
