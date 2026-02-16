package io.appform.conductor.core.id.constraints.impl;

public interface ShardProvider {

    int shardId(String key);

}
