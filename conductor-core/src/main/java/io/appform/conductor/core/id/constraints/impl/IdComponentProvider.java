package io.appform.conductor.server.id.constraints.impl;


import io.appform.conductor.server.id.Id;

public interface IdComponentProvider<T> {

    T provide(Id id);

}