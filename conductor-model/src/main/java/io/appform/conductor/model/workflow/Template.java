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

package io.appform.conductor.model.workflow;

import lombok.Getter;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Represents a template that needs to be evaluated
 */
@Value
public class Template implements Serializable {

    @Serial
    private static final long serialVersionUID = 4389146888753490928L;

    public enum Type {
        FIXED(Set.of(OutputType.TEXT, OutputType.OBJECT)),
        HANDLEBARS(Set.of(OutputType.TEXT)),
        STRING_SUBSTITUTION(Set.of(OutputType.TEXT)),

        JSON_TEMPLATE(Set.of(OutputType.OBJECT))
        ;

        @Getter
        private final Set<OutputType> outputTypes;

        Type(Set<OutputType> outputTypes) {
            this.outputTypes = outputTypes;
        }
    }

    public enum OutputType {
        TEXT,
        OBJECT
    }

    Type type;
    String template;

    public static Template fixed(String template) {
        return new Template(Type.FIXED, template);
    }
}
