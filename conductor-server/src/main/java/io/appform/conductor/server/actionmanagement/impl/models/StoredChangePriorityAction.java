package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.ticket.TicketPriority;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serial;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue(value = "CHANGE_PRIORITY")
public class StoredChangePriorityAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = -126501566609282753L;

    @Column(name = "ticket_priority")
    @Enumerated(EnumType.STRING)
    private TicketPriority ticketPriority;

    @Builder
    public StoredChangePriorityAction(
            String actionId,
            String name,
            String description,
            TicketPriority ticketPriority,
            StoredCompositionAction parentAction) {
        super(ActionType.CHANGE_PRIORITY, actionId, name, description, parentAction);
        this.ticketPriority = ticketPriority;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
