package io.appform.conductor.server.actionmanagement.impl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.appform.conductor.model.actions.ActionType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Generated;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = StoredAction.ACTION_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint(columnNames = {StoredAction.Fields.actionId})})
@DynamicUpdate
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActionType type;

    @Column(name = "action_id", nullable = false)
    private String actionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ToString.Exclude
    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @JoinColumn(name = "parent_action_id")
    private StoredAction parentAction;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    private Date updated;


    protected StoredAction(ActionType type,
                           String actionId,
                           String name,
                           String description,
                           StoredAction parentAction) {
        this.type = type;
        this.actionId = actionId;
        this.name = name;
        this.description = description;
        this.parentAction = parentAction;
    }

    public abstract <T> T accept(final StoredActionVisitor<T> visitor);




}
