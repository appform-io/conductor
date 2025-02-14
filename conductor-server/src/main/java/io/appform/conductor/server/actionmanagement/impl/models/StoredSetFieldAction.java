package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.TemplateConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Getter
@Setter
@Entity
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = ActionType.SET_FIELD_TEXT)
public class StoredSetFieldAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 3846412829595254898L;

    @Column(name = "field_schema_name", length = Constants.MAX_FIELD_SCHEMA_ID_LENGTH)
    private String fieldSchemaName;

    @Convert(converter = TemplateConverter.class)
    @Column(name = "field_value_template", columnDefinition = "text", length = Constants.MAX_TEMPLATE_LENGTH)
    private Template fieldValueTemplate;

    @Builder
    public StoredSetFieldAction() {
        super(ActionType.SET_FIELD);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredSetFieldAction that = (StoredSetFieldAction) o;
        return Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
