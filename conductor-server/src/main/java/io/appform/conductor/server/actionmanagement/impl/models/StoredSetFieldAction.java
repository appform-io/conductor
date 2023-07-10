package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.ChoicesStringConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
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

    @Column(name = "field_schema_id", length = 45)
    private String fieldSchemaId;

    @Embedded
    private StoredFieldValue storedfieldValue;

    @Builder
    public StoredSetFieldAction() {
        super(ActionType.SET_FIELD);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }


    public StoredSetFieldAction setFieldValue(FieldValue fieldValue) {
        this.storedfieldValue = toStoredField(fieldValue);
        return this;
    }

    private StoredFieldValue toStoredField(FieldValue fieldValue) {

        StoredFieldValue storedFieldValue = StoredFieldValue.builder()
                .type(fieldValue.getType())
                .build();

        fieldValue.accept(new FieldValueVisitor<Void>() {
            @Override
            public Void visit(StringFieldValue stringFieldValue) {
                storedFieldValue.setStringValue(stringFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(ChoiceFieldValue choiceFieldValue) {
                storedFieldValue.setChoiceValue(choiceFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(BooleanFieldValue booleanFieldValue) {
                storedFieldValue.setBooleanValue(booleanFieldValue.isValue());
                return null;
            }

            @Override
            public Void visit(NumberFieldValue numberFieldValue) {
                storedFieldValue.setNumberValue(numberFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(LocationFieldValue locationFieldValue) {
                storedFieldValue.setLocationLatValue(locationFieldValue.getLat())
                        .setLocationLonValue(locationFieldValue.getLon());
                return null;

            }

            @Override
            public Void visit(DateFieldValue dateFieldValue) {
                storedFieldValue.setDateValue(dateFieldValue.getValue());
                return null;
            }
        });
        return storedFieldValue;
    }

    public FieldValue getFieldValue() {
        return switch (storedfieldValue.getType()) {
            case STRING -> new StringFieldValue(storedfieldValue.getStringValue());
            case CHOICE -> new ChoiceFieldValue(storedfieldValue.getChoiceValue());
            case BOOLEAN -> new BooleanFieldValue(storedfieldValue.isBooleanValue());
            case NUMBER -> new NumberFieldValue(storedfieldValue.getNumberValue());
            case LOCATION -> new LocationFieldValue(storedfieldValue.getLocationLatValue(), storedfieldValue.getLocationLonValue());
            case DATE -> new DateFieldValue(storedfieldValue.getDateValue());
        };
    }


    //TODO: check if we should use single class ?

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoredFieldValue implements Serializable {

        @Serial
        private static final long serialVersionUID = 6247847187624422131L;

        @Column(name = "field_type", length = 45)
        @Enumerated(EnumType.STRING)
        private FieldType type;

        @Column(name = "string_value")
        private String stringValue;

        @Column(name = "boolean_value")
        private boolean booleanValue;

        @Column(name = "number_value")
        private double numberValue;

        @Column(name = "location_lat_value")
        private double locationLatValue;

        @Column(name = "location_lon_value")
        private double locationLonValue;

        @Column(name = "choices_value")
        @Convert(converter = ChoicesStringConverter.class)  //TODO: Check if we can reuse this ?
        private List<String> choiceValue;

        @Column(name = "date_value")
        private Date dateValue;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredSetFieldAction that = (StoredSetFieldAction) o;
        return Objects.equals(getId(), that.getId())  && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
