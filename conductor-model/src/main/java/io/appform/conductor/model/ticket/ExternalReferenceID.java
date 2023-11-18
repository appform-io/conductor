package io.appform.conductor.model.ticket;

import lombok.Value;

import javax.validation.constraints.NotEmpty;


@Value
public class ExternalReferenceID {

    @NotEmpty
    String source;

    @NotEmpty
    String refId;
}
