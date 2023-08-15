package io.appform.conductor.model.actions;

import lombok.Value;

@Value
public class ActionScope {

    public enum ScopeType {

        STATE,
        TRANSITION,
        GLOBAL
    }

    private ScopeType type;
    private String referenceId;

}
