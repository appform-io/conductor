package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.*;

import javax.persistence.*;import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = StoredAction.ACTION_TABLE_NAME, indexes = {
        @Index(name = "idx_scope_reference_id", columnList = "scope_reference_id")
})
@Getter
@Setter
@ToString
@FieldNameConstants
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = StoredAction.Fields.type)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class StoredAction implements Serializable {

    public static final String ACTION_TABLE_NAME = "actions";

    @Serial
    private static final long serialVersionUID = -6346454270217160408L;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, insertable=false, updatable = false, length = 45)
    private final ActionType type;

    @Id
    @LookupKey
    @Column(name = "action_id", nullable = false, length = Constants.MAX_ACTION_ID_LENGTH)
    private String actionId;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 45)
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id", length = 255)
    private String scopeReferenceId;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    public abstract <T> T accept(final StoredActionVisitor<T> visitor);




}
