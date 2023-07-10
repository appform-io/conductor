package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.workflow.Template;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Getter
@Setter
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = ActionType.ADD_COMMENT_TEXT)
public class StoredAddCommentAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 6008240787741643592L;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_template_type", length = 45)
    private Template.Type contentTemplateType;

    @Column(name = "content_template", length = 1023)
    private String contentTemplate;

    public StoredAddCommentAction() {
        super(ActionType.ADD_COMMENT);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredAddCommentAction that = (StoredAddCommentAction) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
