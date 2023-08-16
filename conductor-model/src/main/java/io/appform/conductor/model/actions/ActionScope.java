package io.appform.conductor.model.actions;

import lombok.Value;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Value
public class ActionScope {

    public enum ScopeType {

        STATE,
        TRANSITION,
        TICKET,
        GLOBAL
    }

    @NotNull
    private ScopeType type;

    @Nullable
    private String referenceId;

}
