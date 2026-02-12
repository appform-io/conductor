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

package io.appform.conductor.server.utils.persistence;

import com.google.common.base.Strings;
import io.appform.conductor.model.auth.Permission;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a set of {@link Permission} to string and back
 */
@Converter
public class PermissionsConverter implements AttributeConverter<Set<Permission>, String> {
    @Override
    public String convertToDatabaseColumn(Set<Permission> attribute) {
        if(attribute == null) {
            return null;
        }
        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<Permission> convertToEntityAttribute(String dbData) {
        if(Strings.isNullOrEmpty(dbData)) {
            return Set.of();
        }
        return Arrays.stream(dbData.split(","))
                .map(Permission::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
