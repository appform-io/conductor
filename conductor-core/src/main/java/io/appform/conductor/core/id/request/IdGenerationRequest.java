package io.appform.conductor.core.id.request;

import io.appform.conductor.core.id.constraints.IdValidationConstraint;
import io.appform.conductor.core.id.formatter.IdFormatter;
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
