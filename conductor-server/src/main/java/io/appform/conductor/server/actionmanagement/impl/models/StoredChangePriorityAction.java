package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.ticket.TicketPriority;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = "CHANGE_PRIORITY")
public class StoredChangePriorityAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = -126501566609282753L;

    @Column(name = "priority", length = 45)
    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Builder
    public StoredChangePriorityAction(
            String actionId,
            String name,
            String description,
            TicketPriority priority,
            StoredCompositionAction parentAction) {
        super(ActionType.CHANGE_PRIORITY, actionId, name, description, parentAction);
        this.priority = priority;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredChangePriorityAction that = (StoredChangePriorityAction) o;
        return Objects.equals(getId(), that.getId())  && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
