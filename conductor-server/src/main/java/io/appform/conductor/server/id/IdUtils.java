package io.appform.conductor.server.id;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.server.id.constraints.impl.SameShardConstraint;
import io.appform.conductor.server.id.constraints.impl.ShardProvider;
import io.appform.dropwizard.sharding.utils.ShardCalculator;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Singleton
public class IdUtils {

    private final ShardCalculator<String> shardCalculator;

    @Inject
    public IdUtils(ShardCalculator<String> shardCalculator) {
        this.shardCalculator = shardCalculator;
    }

    public String createUserInSameShard(String email) {
        return createIdInSameShard("U", email, shardCalculator::shardId);
    }

    @MonitoredFunction
    private String createIdInSameShard(String prefix, String referenceId, ShardProvider shardProvider) {
        Optional<String> idOptional = IdGenerator.generateWithConstraints(prefix,
                Collections.singletonList(new SameShardConstraint(
                        Sets.newHashSet(
                                id -> shardProvider.shardId(id.getId()),
                                id -> shardProvider.shardId(referenceId)
                        )))
        ).map(Id::getId);
        if (idOptional.isPresent()) {
            return idOptional.get();
        } else {
            log.error("IDGenerationFailure for prefix: {}, referenceId: {}", prefix, referenceId);
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.UNHANDLED_SERVER_ERROR)
                    .build();
        }
    }

}
