package io.appform.conductor.model.actions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Scope {
    public static final String GLOBAL_STATE_REF_ID = "__GLOBAL__";
    public static final Scope GLOBAL = new Scope(ScopeType.GLOBAL, GLOBAL_STATE_REF_ID);

    public enum ScopeType {

        STATE,
        TRANSITION,
        WORKFLOW,
        TICKET,
        TASK,
        GLOBAL,
    }

    @NotNull
    ScopeType type;

    @Nullable
    String referenceId;

    public static Scope create(@NonNull ScopeType type, String referenceId) {
        return type == ScopeType.GLOBAL
            ? GLOBAL
            : new Scope(type, referenceId);
    }

}
