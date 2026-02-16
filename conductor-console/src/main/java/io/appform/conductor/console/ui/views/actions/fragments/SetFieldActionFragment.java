package io.appform.conductor.console.ui.views.actions.fragments;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.Scope;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SetFieldActionFragment extends TemplateView {
    Scope scope;
    Action currentAction;

    public SetFieldActionFragment(
            Scope scope,
            Action currentAction) {
        super("templates/actions/fragments/set-field.hbs");
        this.scope = scope;
        this.currentAction = currentAction;
    }
}
