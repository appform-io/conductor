package io.appform.conductor.server.id.constraints.impl;

public interface ShardProvider {

    int shardId(String key);

}
