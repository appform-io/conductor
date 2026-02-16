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
package io.appform.conductor.core.config.hz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.Min;

/**
 * AWS EC2 based cluster configuration.
 * See: https://github.com/hazelcast/hazelcast-aws#ecsfargate-configuration
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsClusterDiscoveryConfig extends ClusterDiscoveryConfig {
    @JsonProperty
    private String serviceName;

    @JsonProperty
    private String accessKey;

    @JsonProperty
    private String secretKey;

    @JsonProperty
    private String iamRole;

    @JsonProperty
    private String region;

    @JsonProperty
    private String hostHeader;

    @JsonProperty
    private String securityGroupName;

    @JsonProperty
    @Min(0)
    private int opTimeoutSeconds;

    @JsonProperty
    private boolean isExternalClient;

    public AwsClusterDiscoveryConfig() {
        super(ClusterDiscoveryType.AWS);
    }

    public AwsClusterDiscoveryConfig(
            String serviceName,
            String accessKey,
            String secretKey,
            String iamRole,
            String region,
            String hostHeader,
            String securityGroupName,
            int opTimeoutSeconds,
            boolean isExternalClient) {
        this();
        this.serviceName = serviceName;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.iamRole = iamRole;
        this.region = region;
        this.hostHeader = hostHeader;
        this.securityGroupName = securityGroupName;
        this.opTimeoutSeconds = opTimeoutSeconds;
        this.isExternalClient = isExternalClient;
    }

    @Override
    public <T> T accept(ClusterDiscoveryConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
