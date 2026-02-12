package io.appform.conductor.server.ingressmanagement.impl.models;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.TemplateConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldNameConstants
public class StoredIngressTranslator implements Serializable {

    public static final String INGRESS_TRANSLATOR_TABLE_NAME = "ingress_translator";

    @Serial
    private static final long serialVersionUID = 561558382851249019L;

    @Id
    @LookupKey
    @Column(name = "id", length = Constants.MAX_INGRESS_TRANSLATOR_ID_LENGTH, nullable = false, unique = true)
    private String id;

    @Column(name = "name", nullable = false, length = Constants.MAX_INGRESS_TRANSLATOR_ID_LENGTH)
    private String name;

    @Column(name = "description", nullable = false, length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 45)
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id", length = 255)
    private String scopeReferenceId;

    @Column(name = "ticket_id_path", nullable = false, length = 255)
    private String ticketIdPath;

    @SuppressWarnings("java:S1948")
    @Convert(converter = TemplateConverter.class)
    @Column(name = "template", columnDefinition = "text", length = Constants.MAX_TEMPLATE_LENGTH)
    private Template template;

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
        if (!(o instanceof StoredIngressTranslator that)) {
            return false;
        }

        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer()
                .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = o instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer()
                .getPersistentClass() : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }

        return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(ticketIdPath, that.ticketIdPath)
                && Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(scopeType, that.scopeType) && Objects.equals(scopeReferenceId, that.scopeReferenceId)
                && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return this instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
