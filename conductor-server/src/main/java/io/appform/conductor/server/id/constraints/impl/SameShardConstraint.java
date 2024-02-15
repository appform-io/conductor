
package io.appform.conductor.server.id.constraints.impl;

import io.appform.conductor.server.id.Id;
import io.appform.conductor.server.id.constraints.IdValidationConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class SameShardConstraint implements IdValidationConstraint {

    private final Set<IdComponentProvider> components;

    @Override
    public boolean isValid(Id id) {
        return components.stream().map(x -> x.provide(id)).collect(Collectors.toSet()).size() == 1;
    }
}