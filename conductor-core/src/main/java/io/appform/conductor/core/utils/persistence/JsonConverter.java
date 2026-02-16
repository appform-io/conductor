/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.core.utils.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.persistence.AttributeConverter;

/**
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED,
        onConstructor_ = @Inject)
public abstract class JsonConverter<T> implements AttributeConverter<T, String> {
    private final TypeReference<T> typeReference = new TypeReference<T>() {
    };
    private final ObjectMapper mapper;

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(T attribute) {
        return mapper.writeValueAsString(attribute);
    }

    @Override
    @SneakyThrows
    public T convertToEntityAttribute(String dbData) {
        return mapper.readValue(dbData, typeReference);
    }
}
