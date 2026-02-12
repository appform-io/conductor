/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.conductor.server.config.hz;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "SIMPLE", value = SimpleClusterDiscoveryConfig.class),
        @JsonSubTypes.Type(name = "AWS", value = AwsClusterDiscoveryConfig.class),
        @JsonSubTypes.Type(name = "AWS_ECS", value = AwsECSDiscoveryConfig.class),
        @JsonSubTypes.Type(name = "KUBERNETES", value = KubernetesClusterDiscoveryConfig.class),
})
@NoArgsConstructor
public abstract class ClusterDiscoveryConfig {

    @NotNull
    private ClusterDiscoveryType type = ClusterDiscoveryType.SIMPLE;

    protected ClusterDiscoveryConfig(final ClusterDiscoveryType type) {
        this.type = type;
    }


    public ClusterDiscoveryType getType() {
        return this.type;
    }

    public abstract <T> T accept(final ClusterDiscoveryConfigVisitor<T> visitor);
}
