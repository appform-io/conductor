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
import com.google.common.net.MediaType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts to and fro {@link com.google.common.net.MediaType}
 */
@Converter
public class MediaTypeConverter implements AttributeConverter<MediaType, String> {
    @Override
    public String convertToDatabaseColumn(MediaType attribute) {
        if(null == attribute) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public MediaType convertToEntityAttribute(String dbData) {
        if(Strings.isNullOrEmpty(dbData)) {
            return null;
        }
        return MediaType.parse(dbData);
    }
}
