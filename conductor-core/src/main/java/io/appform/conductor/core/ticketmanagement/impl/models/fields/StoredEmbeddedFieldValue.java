package io.appform.conductor.core.ticketmanagement.impl.models.fields;

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.core.utils.persistence.StringListConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Embedded class as the same is being used in {@link StoredFieldValue} and {@link io.appform.conductor.server.actionmanagement.impl.models.StoredSetFieldAction}
 *
 */

@Embeddable
@Data
@Builder
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class StoredEmbeddedFieldValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 6247847187624422131L;

    @Column(name = "field_type", length = 45)
    @Enumerated(EnumType.STRING)
    private FieldType type;

    @Column(name = "string_value", length = 512)
    private String stringValue;

    @Column(name = "boolean_value")
    private boolean booleanValue;

    @Column(name = "number_value")
    private double numberValue;

    @Column(name = "location_lat_value")
    private double locationLatValue;

    @Column(name = "location_lon_value")
    private double locationLonValue;

    @Column(name = "choices_value", length = 512)
    @Convert(converter = StringListConverter.class)
    private List<String> choiceValue;

    @Column(name = "date_value")
    private Date dateValue;

    public StoredEmbeddedFieldValue(FieldValue fieldValue) {
        type = fieldValue.getType();
        fieldValue.accept(new FieldValueVisitor<Void>() {
            @Override
            public Void visit(StringFieldValue stringFieldValue) {
                stringValue = stringFieldValue.getValue();
                return null;
            }

            @Override
            public Void visit(ChoiceFieldValue choiceFieldValue) {
                choiceValue = choiceFieldValue.getValue();
                return null;
            }

            @Override
            public Void visit(BooleanFieldValue booleanFieldValue) {
                booleanValue = booleanFieldValue.isValue();
                return null;
            }

            @Override
            public Void visit(NumberFieldValue numberFieldValue) {
                numberValue = numberFieldValue.getValue();
                return null;
            }

            @Override
            public Void visit(LocationFieldValue locationFieldValue) {
                locationLatValue = locationFieldValue.getLat();
                locationLonValue = locationFieldValue.getLon();
                return null;

            }

            @Override
            public Void visit(DateFieldValue dateFieldValue) {
                dateValue = dateFieldValue.getValue();
                return null;
            }
        });
    }

    public FieldValue toFieldValue() {
        return switch (getType()) {
            case STRING -> new StringFieldValue(getStringValue());
            case CHOICE -> new ChoiceFieldValue(getChoiceValue());
            case BOOLEAN -> new BooleanFieldValue(isBooleanValue());
            case NUMBER -> new NumberFieldValue(getNumberValue());
            case LOCATION -> new LocationFieldValue(getLocationLatValue(), getLocationLonValue());
            case DATE -> new DateFieldValue(getDateValue());
        };
    }

}