package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.workflow.Template;
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
@DiscriminatorValue(value = "ADD_COMMENT")
public class StoredAddCommentAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 6008240787741643592L;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_template_type")
    private Template.Type contentTemplateType;

    @Column(name = "content_template")
    private String contentTemplate;

    @Builder
    public StoredAddCommentAction(
            String actionId,
            String name,
            String description,
            Template contentTemplate,
            StoredCompositionAction parentAction) {
        super(ActionType.ADD_COMMENT, actionId, name, description, parentAction);
        this.contentTemplateType = contentTemplate.getType();
        this.contentTemplate = contentTemplate.getTemplate();
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
