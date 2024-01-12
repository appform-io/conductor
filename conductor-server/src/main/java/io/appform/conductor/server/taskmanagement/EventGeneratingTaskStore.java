package io.appform.conductor.server.taskmanagement;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.events.impl.task.TaskCreatedEvent;
import io.appform.conductor.model.events.impl.task.TaskDeletedEvent;
import io.appform.conductor.model.events.impl.task.TaskUpdatedEvent;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.taskmanagement.model.Task;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingTaskStore implements TaskStore {
    private final EventBus eventBus;
    private final TaskStore taskStore;

    @Inject
    public EventGeneratingTaskStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) TaskStore taskStore) {
        this.eventBus = eventBus;
        this.taskStore = taskStore;
    }

    @Override
    public Optional<Task> createOrUpdate(String id, Task task) {
        val res =  taskStore.createOrUpdate(id, task);
        res.ifPresent(newTask -> eventBus.publish(new TaskCreatedEvent(newTask.getId())));
        return res;
    }

    @Override
    public Optional<Task> update(String id, UnaryOperator<Task> updater) {
        val res =  taskStore.update(id, updater);
        res.ifPresent(newTask -> eventBus.publish(new TaskUpdatedEvent(newTask.getId())));
        return res;
    }

    @Override
    public boolean delete(String id) {
        val res = taskStore.delete(id);
        if(res) {
            eventBus.publish(new TaskDeletedEvent(id));
        }
        return res;
    }

    @Override
    public Optional<Task> read(String taskId) {
        return taskStore.read(taskId);
    }

    @Override
    public List<Task> listByIds(List<String> ids) {
        return taskStore.listByIds(ids);
    }

    @Override
    public List<Task> listByScopes(List<Scope> scopes) {
        return taskStore.listByScopes(scopes);
    }
}
