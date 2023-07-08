package io.appform.conductor.server.actionmanagement.impl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.appform.conductor.model.actions.ActionErrorHandlingStrategy;
import io.appform.conductor.model.actions.ActionType;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue(value = "COMPOSITION")
public class StoredCompositionAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 5817562309411421599L;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "parentAction", fetch = FetchType.EAGER)
    private List<StoredAction> actions;


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
            List<StoredAction> actions) {
        super(ActionType.COMPOSITION, actionId, name, description, parentAction);
        this.actions = actions;
        this.actionErrorHandlingStrategy = actionErrorHandlingStrategy;
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
