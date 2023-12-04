package io.appform.conductor.server.ticketmanagement.impl.models;

import io.appform.conductor.model.ticket.TicketRelationship;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

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
    @Column(name = "related_id")
    private String relatedId;

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "related_to_ticket_id")
    private String relatedToTicketId;

    @Column(name = "relationship")
    @Enumerated
    private TicketRelationship relationship;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
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
