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
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.val;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Map;

/**
 * Converts a {@link Rule} to string and back
 */
@Converter
public class RuleConverter implements AttributeConverter<Rule, String> {
    @Override
    public String convertToDatabaseColumn(Rule attribute) {
        if(attribute == null) {
            return null;
        }
        return attribute.getType().name() + "|" + attribute.getRule();
    }

    @Override
    public Rule convertToEntityAttribute(String dbData) {
        if(Strings.isNullOrEmpty(dbData)) {
            return null;
        }
        val parts = dbData.split("\\|", 2);
        ConductorServerUtils.ensureCondition(parts.length == 2,
                                             ConductorErrorCode.DATA_FORMAT_ERROR,
                                             Map.of("type", Rule.class.getSimpleName(),
                                                    "content", dbData));
        return new Rule(Rule.RuleType.valueOf(parts[0]), parts[1]);
    }
}
