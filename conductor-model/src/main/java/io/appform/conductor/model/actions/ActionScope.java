package io.appform.conductor.model.actions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionScope {
    public static final String GLOBAL_STATE_REF_ID = "__GLOBAL__";

    public enum ScopeType {

        STATE,
        TRANSITION,
        TICKET,
        GLOBAL
    }

    @NotNull
    ScopeType type;

    @Nullable
    String referenceId;

    public static ActionScope global() {
        return new ActionScope(ScopeType.GLOBAL, GLOBAL_STATE_REF_ID);
    }

    public static ActionScope build(@NonNull ScopeType type, String referenceId) {
        return type == ScopeType.GLOBAL
            ? global()
            : new ActionScope(type, referenceId);
    }

}
