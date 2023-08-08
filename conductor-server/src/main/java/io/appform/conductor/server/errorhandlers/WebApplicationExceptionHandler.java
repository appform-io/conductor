package io.appform.conductor.server.errorhandlers;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles param validations from jersey and returns standard error
 */
@Slf4j
@Provider
public class WebApplicationExceptionHandler implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        val message = exception.getMessage();
        val response = exception.getResponse();
        if(null != message) {
            log.error("Application error: {}.", message);
        }
        return response;
    }
}
