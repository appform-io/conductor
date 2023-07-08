package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.model.workflow.Template;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@ToString(callSuper = true)
@DiscriminatorValue(value = "WEBHOOK")
public class StoredWebhookAction  extends StoredAction {

    @Serial
    private static final long serialVersionUID = 4616039718420722116L;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type")
    private WebhookAction.CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_mode")
    private WebhookAction.CallMode callMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "url_template_type")
    private Template.Type urlTemplateType;

    @Column(name = "url_template")
    private String urlTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "headers_template_type")
    private Template.Type headersTemplateType;

    @Column(name = "headers_template")
    private String headersTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payload_template_type")
    private Template.Type payloadTemplateType;

    @Column(name = "payload_template")
    private String payloadTemplate;

    @Convert(converter = WebhookActionSuccessCodesConverter.class)
    @Column(name = "success_codes")
    private Set<Integer> successCodes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "timeout_ms")
    private int timeoutMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_strategy")
    private WebhookAction.RetryStrategy retryStrategy;

    @Column(name = "num_retries")
    private int numRetries;

    @Builder
    public StoredWebhookAction(
            String actionId,
            String name,
            String description,
            WebhookAction.CallType callType,
            WebhookAction.CallMode callMode,
            Template urlTemplate,
            Template headersTemplate,
            Template payloadTemplate,
            Set<Integer> successCodes,
            String mimeType,
            int timeoutMs,
            WebhookAction.RetryStrategy retryStrategy,
            int numRetries,
            StoredCompositionAction parentAction) {
        super(ActionType.WEBHOOK, actionId, name, description, parentAction);
        this.callType = callType;
        this.callMode = callMode;
        this.urlTemplateType = urlTemplate.getType();
        this.urlTemplate = urlTemplate.getTemplate();
        this.headersTemplateType = headersTemplate.getType();
        this.headersTemplate = headersTemplate.getTemplate();
        this.payloadTemplateType = payloadTemplate.getType();
        this.payloadTemplate = payloadTemplate.getTemplate();
        this.successCodes = successCodes;
        this.mimeType = mimeType;
        this.timeoutMs = timeoutMs;
        this.retryStrategy = retryStrategy;
        this.numRetries = numRetries;
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
        return Objects.equals(getId(), that.getId())  && Objects.equals(getActionId(), that.getActionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
