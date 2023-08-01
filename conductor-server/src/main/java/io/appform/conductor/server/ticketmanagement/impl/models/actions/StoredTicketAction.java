package io.appform.conductor.server.ticketmanagement.impl.models.actions;

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
@Table(name = StoredTicketAction.TICKET_ACTION_TABLE_NAME,
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_action", columnNames = {"ticket_id", "action_id"})
        })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredTicketAction implements Serializable {
    public static final String TICKET_ACTION_TABLE_NAME = "ticket_actions";

    @Serial
    private static final long serialVersionUID = -5522495724716864350L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "action_id", nullable = false)
    private String actionId;

    @Column(name = "deleted")
    private boolean deleted;

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
        StoredTicketAction that = (StoredTicketAction) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
