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

@Setter
@Getter
@Entity
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = ActionType.ROUTE_TO_GROUP_TEXT)
public class StoredRouteToGroupAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 1128463869091495127L;

    @Column(name = "group_id", length = 127)
    private String groupId;

    @Builder
    public StoredRouteToGroupAction() {
        super(ActionType.ROUTE_TO_GROUP);
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
