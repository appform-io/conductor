package io.appform.conductor.server.schemamanagement.impl;

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.SchemaSummary;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.*;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class EventGeneratingSchemaStore implements SchemaStore {
    private final EventBus eventBus;
    private final SchemaStore schemaStore;

    @Inject
    public EventGeneratingSchemaStore(EventBus eventBus, @Named("root") SchemaStore schemaStore) {
        this.eventBus = eventBus;
        this.schemaStore = schemaStore;
    }

    @Override
    public Optional<SchemaSummary> create(String name, String description) {
        val res = schemaStore.create(name, description);
        res.ifPresent(schemaSummary -> eventBus.publish(new SchemaCreatedEvent(schemaSummary.getId())));
        return res;
    }

    @Override
    public Optional<SchemaSummary> getSummary(String schemaId) {
        return schemaStore.getSummary(schemaId);
    }

    @Override
    public Optional<Schema> get(String schemaId) {
        return schemaStore.get(schemaId);
    }

    @Override
    public List<SchemaSummary> list() {
        return schemaStore.list();
    }

    @Override
    public Optional<SchemaSummary> updateDescription(String schemaId, String description) {
        return schemaStore.updateDescription(schemaId, description);
    }

    @Override
    public Optional<SchemaSummary> updateState(String schemaId, SchemaState state) {
        val res = schemaStore.updateState(schemaId, state);
        res.filter(schemaSummary -> schemaSummary.getState() == state)
                .ifPresent(schemaSummary -> eventBus.publish(new SchemaStateUpdatedEvent(schemaSummary.getId(),
                        schemaSummary.getState())));
        return res;
    }

    @Override
    public Optional<FieldSchema> addField(String schemaId, String fieldId, FieldSchema schema) {
        val res = schemaStore.addField(schemaId, fieldId, schema);
        res.ifPresent(fieldSchema -> eventBus.publish(new SchemaFieldAddedEvent(schemaId, fieldSchema.getId())));
        return res;
    }

    @Override
    public Optional<FieldSchema> getField(String schemaId, String fieldId) {
        return schemaStore.getField(schemaId, fieldId);
    }

    @Override
    public Optional<FieldSchema> updateField(String schemaId, String fieldId, FieldSchema updated) {
        val res = schemaStore.updateField(schemaId, fieldId, updated);
        res.ifPresent(fieldSchema -> eventBus.publish(new SchemaFieldUpdatedEvent(schemaId, fieldSchema.getId())));
        return res;
    }

    @Override
    public boolean deleteField(String schemaId, String fieldId) {
        val res = schemaStore.deleteField(schemaId, fieldId);
        if(res) {
            eventBus.publish(new SchemaFieldDeletedEvent(schemaId, fieldId));
        }
        return res;
    }
}
