package io.appform.conductor.server.id.request;

import io.appform.conductor.server.id.constraints.IdValidationConstraint;
import io.appform.conductor.server.id.formatter.IdFormatter;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class IdGenerationRequest {

    String prefix;
    String domain;
    boolean skipGlobal;
    List<IdValidationConstraint> constraints;
    IdFormatter idFormatter;

}
