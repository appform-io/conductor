package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.ChoicesStringConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue(value = "SET_FIELD")
public class StoredSetFieldAction extends StoredAction {

    @Serial
    private static final long serialVersionUID = 3846412829595254898L;

    @Column(name = "field_schema_id")
    private String fieldSchemaId;

    @Embedded
    private StoredFieldValue fieldValue;

    @Builder
    public StoredSetFieldAction(
            String actionId,
            String name,
            String description,
            String fieldSchemaId,
            FieldValue fieldValue,
            StoredCompositionAction parentAction) {
        super(ActionType.SET_FIELD, actionId, name, description, parentAction);
        this.fieldSchemaId = fieldSchemaId;
        this.fieldValue = toStoredField(fieldValue);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }


    public StoredFieldValue toStoredField(FieldValue fieldValue) {

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
                        .setLocationLatValue(locationFieldValue.getLon());
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


    //TODO: check if we should use single class ?

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoredFieldValue implements Serializable {

        @Serial
        private static final long serialVersionUID = 6247847187624422131L;

        @Column(name = "field_type")
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


}
