package io.appform.conductor.model.ingress;

import io.appform.conductor.model.workflow.Template;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Value
public class IngressTranslator implements Serializable {
    @Serial
    private static final long serialVersionUID = 3557840487483176051L;
    String id;
    String name;
    Template template;
    Date created;
    Date updated;
}
