package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serial;
import java.util.Objects;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = "ROUTE_TO_GROUP")
public class StoredRouteToGroupAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 1128463869091495127L;

    @Column(name = "group_id", length = 127)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredRouteToGroupAction that = (StoredRouteToGroupAction) o;
        return Objects.equals(getId(), that.getId()) &&  Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
