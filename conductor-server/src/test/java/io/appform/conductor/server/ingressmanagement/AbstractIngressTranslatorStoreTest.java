package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.workflow.Template;
import lombok.val;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractIngressTranslatorStoreTest {

    protected void storeFunctionality(IngressTranslatorStore ingressTranslatorStore) {
        val inputIngressTranslatorWithHandlebars = new IngressTranslator("id", "name",
                new Template(Template.Type.HANDLEBARS, "{{phone}}"), null, null);
        val ingressTranslatorWithHandlebars = ingressTranslatorStore.createOrUpdate(inputIngressTranslatorWithHandlebars).orElse(null);
        assertNotNull(ingressTranslatorWithHandlebars);
        assertEquals(Template.Type.HANDLEBARS, ingressTranslatorWithHandlebars.getTemplate().getType());

        val outputIngressTranslatorWithHandlebars = ingressTranslatorStore.read("id").orElse(null);
        assertNotNull(outputIngressTranslatorWithHandlebars);
        assertEquals(Template.Type.HANDLEBARS, outputIngressTranslatorWithHandlebars.getTemplate().getType());

        val inputIngressTranslatorWithFixed = new IngressTranslator("id", "name",
                new Template(Template.Type.FIXED, "XXXXXXXX"), null, null);

        val ingressTranslatorWithFixed = ingressTranslatorStore.createOrUpdate(inputIngressTranslatorWithFixed).orElse(null);
        assertNotNull(ingressTranslatorWithFixed);
        assertEquals(Template.Type.FIXED,ingressTranslatorWithFixed.getTemplate().getType());

        val outputIngressTranslatorWithFixed= ingressTranslatorStore.read("id").orElse(null);
        assertNotNull(outputIngressTranslatorWithFixed);
        assertEquals(Template.Type.FIXED, outputIngressTranslatorWithFixed.getTemplate().getType());
    }
}
