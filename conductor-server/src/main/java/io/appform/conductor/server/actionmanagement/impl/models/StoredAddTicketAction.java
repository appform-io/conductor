package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serial;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue(value = "ADD_TICKET_ACTION")
public class StoredAddTicketAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = -282656827461451925L;

    @Column(name = "ticket_action_id")
    private String ticketActionId;

    @Builder
    public StoredAddTicketAction(
            String actionId,
            String name,
            String description,
            String ticketActionId,
            StoredCompositionAction parentAction) {
        super(ActionType.ADD_TICKET_ACTION, actionId, name, description, parentAction);
        this.ticketActionId = ticketActionId;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
