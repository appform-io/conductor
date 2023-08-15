package io.appform.conductor.model.actions;

import lombok.Value;

import javax.annotation.Nullable;

@Value
public class ActionScope {

    public enum ScopeType {

        STATE,
        TRANSITION,
        GLOBAL
    }

    private ScopeType type;

    @Nullable
    private String referenceId;

}
