package io.appform.conductor.model.events.impl.skill;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.SKILL_VALUE_UPDATED)
@SuperBuilder
@Jacksonized
public class SkillValueUpdatedEvent extends Event {
    String skillValueId;
    String value;

    public SkillValueUpdatedEvent(String skillId, String skillValueId, String value) {
        super(EventType.SKILL_VALUE_UPDATED, ReferredObjectType.SKILL, skillId);
        this.skillValueId = skillValueId;
        this.value = value;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}