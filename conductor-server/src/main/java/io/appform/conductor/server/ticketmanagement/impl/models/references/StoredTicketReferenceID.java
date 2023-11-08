package io.appform.conductor.server.ticketmanagement.impl.models.references;

import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = StoredTicketReferenceID.TICKET_REFERENCES_TABLE_NAME,
        indexes = {
                @Index(name = "idx_source_value", columnList = "source,ref_id"),
        })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredTicketReferenceID implements Serializable {

    public static final String TICKET_REFERENCES_TABLE_NAME = "ticket_references";
    @Serial
    private static final long serialVersionUID = -4478523159529973547L;

    @Id
    @EmbeddedId
    private StoredTicketReferenceIDPk storedTicketReferenceIDPk;

    @ManyToOne
    @JoinColumn(name = "ticket_id", referencedColumnName = "ticket_id")
    @MapsId("ticket_id")
    private StoredTicketSkeleton ticket;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredTicketReferenceID that = (StoredTicketReferenceID) o;
        return Objects.equals(getStoredTicketReferenceIDPk(), that.getStoredTicketReferenceIDPk());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}


