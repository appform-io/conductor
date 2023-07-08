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
@DiscriminatorValue(value = "ROUTE_TO_GROUP")
public class StoredRouteToGroupAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 1128463869091495127L;

    @Column(name = "group_id")
    private String groupId;

    @Builder
    public StoredRouteToGroupAction(
            String actionId,
            String name,
            String description,
            String groupId,
            StoredCompositionAction parentAction) {
        super(ActionType.ROUTE_TO_GROUP, actionId, name, description, parentAction);
        this.groupId = groupId;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
