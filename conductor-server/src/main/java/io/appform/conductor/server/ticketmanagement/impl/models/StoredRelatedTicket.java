package io.appform.conductor.server.ticketmanagement.impl.models;

import io.appform.conductor.model.ticket.TicketRelationship;
import io.appform.conductor.server.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = StoredRelatedTicket.RELATED_TICKET_TABLE_NAME,
        indexes = {
                @Index(name = "idx_ticket_relation", columnList = "ticket_id,relationship"),
        })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredRelatedTicket implements Serializable {
    public static final String RELATED_TICKET_TABLE_NAME = "related_tickets";

    @Serial
    private static final long serialVersionUID = -1226578157641576480L;

    @Id
    @Column(name = "related_id", length = Constants.MAX_TICKET_RELATED_ID_LENGTH)
    private String relatedId;

    @Column(name = "ticket_id", length = Constants.MAX_TICKET_ID_LENGTH)
    private String ticketId;

    @Column(name = "related_to_ticket_id", length = Constants.MAX_TICKET_ID_LENGTH)
    private String relatedToTicketId;

    @Column(name = "relationship", length = 45)
    @Enumerated(EnumType.STRING)
    private TicketRelationship relationship;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredRelatedTicket that = (StoredRelatedTicket) o;
        return Objects.equals(getRelatedId(), that.getRelatedId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}
