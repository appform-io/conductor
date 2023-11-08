package io.appform.conductor.server.ticketmanagement.impl.models.references;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@FieldNameConstants
@Embeddable
public class StoredTicketReferenceIDPk implements Serializable {
    @Serial
    private static final long serialVersionUID = -6608212807817814424L;

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "source")
    private String source;

    @Column(name = "ref_id")
    private String refId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredTicketReferenceIDPk that = (StoredTicketReferenceIDPk) o;
        return Objects.equals(ticketId, that.getTicketId())
                && Objects.equals(source, that.getSource())
                && Objects.equals(refId, that.getRefId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, source, refId);
    }
}