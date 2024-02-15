/**
 * Copyright 2016 Flipkart Internet Pvt. Ltd.
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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterDiscoveryConfig extends ClusterDiscoveryConfig {


    @JsonProperty
    @Builder.Default
    private boolean disableMulticast = false;

    public KubernetesClusterDiscoveryConfig() {
        super(ClusterDiscoveryType.KUBERNETES);
    }

    public KubernetesClusterDiscoveryConfig(boolean disableMulticast) {
        this();
        this.disableMulticast = disableMulticast;
    }

    @Override
    public <T> T accept(ClusterDiscoveryConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
