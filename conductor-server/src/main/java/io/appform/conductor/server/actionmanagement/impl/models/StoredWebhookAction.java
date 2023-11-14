package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.persistence.TemplateConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = ActionType.WEBHOOK_TEXT)
public class StoredWebhookAction  extends StoredAction {

    @Serial
    private static final long serialVersionUID = 4616039718420722116L;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", length = 45)
    private WebhookAction.CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_mode", length = 45)
    private WebhookAction.CallMode callMode;

    @SuppressWarnings("java:S1948")
    @Convert(converter = TemplateConverter.class)
    @Column(name = "url_template", length = 1023)
    private Template urlTemplate;

    @OneToMany(mappedBy = "action",  fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<StoredWebhookActionHeaderTemplate> headerTemplates;

    @SuppressWarnings("java:S1948")
    @Convert(converter = TemplateConverter.class)
    @Column(name = "payload_template", length = 2047)
    private Template payloadTemplate;

    @Convert(converter = WebhookActionSuccessCodesConverter.class)
    @Column(name = "success_codes", length = 127)
    private Set<Integer> successCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "mime_type", length = 127)
    private WebhookAction.MimeType mimeType;

    @Column(name = "timeout_ms")
    private int timeoutMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_strategy", length = 45)
    private WebhookAction.RetryStrategy retryStrategy;

    @Column(name = "num_retries")
    private int numRetries;

    public StoredWebhookAction() {
        super(ActionType.WEBHOOK);
    }

    @Override
    public <T> T accept(StoredActionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredWebhookAction that = (StoredWebhookAction) o;
        return Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
