package io.appform.conductor.server.ui.views.manage;

import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IngressTranslatorListView extends BaseLoggedInView {
    List<IngressTranslator> translators;

    public IngressTranslatorListView(User currentUser, List<IngressTranslator> translators) {
        super("templates/manage/ingress-translator-list.hbs", currentUser, new ObjectReference(ReferredObjectType.INGRESS_TRANSLATOR, null));
        this.translators = translators;
    }
}
