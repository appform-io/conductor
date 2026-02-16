package io.appform.conductor.core.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.core.utils.persistence.TemplateConverter;
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

    @SuppressWarnings("java:S1948")
    @Convert(converter = TemplateConverter.class)
    @Column(name = "content_template", columnDefinition = "text", length = Constants.MAX_TEMPLATE_LENGTH)
    private Template contentTemplate;

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
        return Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
