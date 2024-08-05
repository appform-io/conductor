package io.appform.conductor.server.ui.views.manage;

import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IngressTranslatorView extends BaseLoggedInView {
    IngressTranslator translator;

    public IngressTranslatorView(User currentUser, IngressTranslator translator) {
        super("templates/manage/ingress-translator-details.hbs", currentUser,
                null != translator
                        ? new ObjectReference(ReferredObjectType.INGRESS_TRANSLATOR, translator.getId())
                        : null);
        this.translator = translator;
    }
}
