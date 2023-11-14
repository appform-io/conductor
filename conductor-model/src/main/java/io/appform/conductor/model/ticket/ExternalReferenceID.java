package io.appform.conductor.model.ticket;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


@Value
public class ExternalReferenceID {

    @NotNull
    @NotEmpty
    private String source;

    @NotNull
    @NotEmpty
    private String refId;
}
