package io.appform.conductor.server.actionmanagement.impl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.persistence.TemplateConverter;
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
@Table(name = StoredWebhookActionHeaderTemplate.WEBHOOK_ACTION_HEADER_TEMPLATE_TABLE,
        uniqueConstraints = @UniqueConstraint(name = "uk_action_id_name", columnNames = {"action_id", "name"}))
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredWebhookActionHeaderTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = -2573719655378348497L;

    public static final String WEBHOOK_ACTION_HEADER_TEMPLATE_TABLE = "webhook_action_header_templates";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "action_id")
    private StoredWebhookAction action;

    @Column(name = "name")
    private String name;

    @SuppressWarnings("java:S1948")
    @Convert(converter = TemplateConverter.class)
    @Column(name = "template" , length = 2047)
    private Template template;

    @Column(name = "active")
    private boolean active;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = "timestamp")
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp")
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoredWebhookActionHeaderTemplate that = (StoredWebhookActionHeaderTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
