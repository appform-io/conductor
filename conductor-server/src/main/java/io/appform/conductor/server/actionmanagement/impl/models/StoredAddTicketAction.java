package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serial;
import java.util.Objects;

@Entity
@Getter
@Setter
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = ActionType.ADD_TICKET_ACTION_TEXT)
public class StoredAddTicketAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = -282656827461451925L;

    @Column(name = "ticket_action_id", length = 45)
    private String ticketActionId;

    public StoredAddTicketAction() {
        super(ActionType.ADD_TICKET_ACTION);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredAddTicketAction that = (StoredAddTicketAction) o;
        return Objects.equals(getId(), that.getId())  && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
