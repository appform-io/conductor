package io.appform.conductor.server.actionmanagement.impl.models;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class WebhookActionSuccessCodesConverter implements AttributeConverter<Set<Integer>, String> {
    @Override
    public String convertToDatabaseColumn(Set<Integer> attribute) {
        return null == attribute ? null : attribute.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
    }

    @Override
    public Set<Integer> convertToEntityAttribute(String dbData) {
        return Arrays.stream(Objects.requireNonNullElse(dbData, "").split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }
}
