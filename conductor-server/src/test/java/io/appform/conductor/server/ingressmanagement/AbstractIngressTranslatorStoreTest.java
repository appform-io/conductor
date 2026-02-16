package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.core.ingressmanagement.IngressTranslatorStore;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.core.utils.ConductorServerUtils;
import lombok.val;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractIngressTranslatorStoreTest {

    protected void storeFunctionality(IngressTranslatorStore ingressTranslatorStore) {
        val name = "id1";
        val id = ConductorServerUtils.readableId(name);
        val handlerbarsTemplate = new Template(Template.Type.HANDLEBARS, "{{phone}}");
        val fixedTemplate = new Template(Template.Type.FIXED, "XXXXXXXX");
        val scope = Scope.create(Scope.ScopeType.WORKFLOW, "WF1");
        val ingressTranslatorWithHandlebars = ingressTranslatorStore.createOrUpdate(name, "description", "/ticketId", handlerbarsTemplate, scope).orElse(null);
        assertNotNull(ingressTranslatorWithHandlebars);
        assertEquals(Template.Type.HANDLEBARS, ingressTranslatorWithHandlebars.getTemplate().getType());

        val outputIngressTranslatorWithHandlebars = ingressTranslatorStore.read(id).orElse(null);
        assertNotNull(outputIngressTranslatorWithHandlebars);
        assertEquals(Template.Type.HANDLEBARS, outputIngressTranslatorWithHandlebars.getTemplate().getType());

        val ingressTranslatorWithFixed = ingressTranslatorStore.createOrUpdate(name, "description2", "/ticketId", fixedTemplate, scope).orElse(null);
        assertNotNull(ingressTranslatorWithFixed);
        assertEquals(Template.Type.FIXED,ingressTranslatorWithFixed.getTemplate().getType());

        val outputIngressTranslatorWithFixed= ingressTranslatorStore.read(id).orElse(null);
        assertNotNull(outputIngressTranslatorWithFixed);
        assertEquals(Template.Type.FIXED, outputIngressTranslatorWithFixed.getTemplate().getType());
    }
}
