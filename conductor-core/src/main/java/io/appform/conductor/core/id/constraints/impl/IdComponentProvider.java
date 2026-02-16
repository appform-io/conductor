package io.appform.conductor.core.id.constraints.impl;


import io.appform.conductor.core.id.Id;

public interface IdComponentProvider<T> {

    T provide(Id id);

}