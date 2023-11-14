package io.appform.conductor.server.actionmanagement.impl.models;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.*;

import javax.persistence.*;import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = StoredAction.ACTION_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"action_id"})})
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
    @Column(name = "action_id", nullable = false, length = 45)
    private String actionId;

    @Column(name = "name", nullable = false, length = 127)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 45)
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id", length = 45)
    private String scopeReferenceId;

    @Column
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = "timestamp")
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp")
    private Date updated;

    public abstract <T> T accept(final StoredActionVisitor<T> visitor);




}
