package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttachmentAddedEvent extends Event {
    String attachmentId;
    String ticketId;

    @Builder
    public AttachmentAddedEvent(String ticketId, String attachmentId) {
        super(EventType.ATTACHMENT_ADDED);
        this.attachmentId = attachmentId;
        this.ticketId = ticketId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}