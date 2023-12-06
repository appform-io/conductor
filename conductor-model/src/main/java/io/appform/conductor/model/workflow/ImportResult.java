package io.appform.conductor.model.workflow;

import lombok.Value;

@Value
public class ImportResult<T> {
    T data;
    boolean success;
    String error;
}
