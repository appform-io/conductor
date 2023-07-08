package io.appform.conductor.server.actionmanagement.impl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.appform.conductor.model.actions.ActionErrorHandlingStrategy;
import io.appform.conductor.model.actions.ActionType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = "COMPOSITION")
public class StoredCompositionAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 5817562309411421599L;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "parentAction", fetch = FetchType.EAGER)
    private List<StoredAction> children;


    @Column(name = "action_error_handling")
    @Enumerated(EnumType.STRING)
    private ActionErrorHandlingStrategy actionErrorHandlingStrategy;


    @Builder
    public StoredCompositionAction(
            String actionId,
            String name,
            String description,
            ActionErrorHandlingStrategy actionErrorHandlingStrategy,
            StoredCompositionAction parentAction,
            List<StoredAction> children) {
        super(ActionType.COMPOSITION, actionId, name, description, parentAction);
        this.children = children;
        this.actionErrorHandlingStrategy = actionErrorHandlingStrategy;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredCompositionAction that = (StoredCompositionAction) o;
        return Objects.equals(getId(), that.getId())  && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
