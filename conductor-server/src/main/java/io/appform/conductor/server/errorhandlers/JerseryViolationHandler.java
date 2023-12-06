package io.appform.conductor.server.errorhandlers;

import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.jersey.validation.JerseyViolationException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.JerseyManaged;
import ru.vyarus.dropwizard.guice.module.installer.install.binding.LazyBinding;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.UUID;

/**
 * Handles param validations from jersey and returns standard error
 */
@Slf4j
@Provider
@LazyBinding
@JerseyManaged
public class JerseryViolationHandler implements ExceptionMapper<JerseyViolationException> {

    @Context
    private UriInfo uriInfo;


    @Override
    public Response toResponse(JerseyViolationException exception) {
        val message = exception.getMessage();
        if (uriInfo.getPath().startsWith("ui/")) {
            return ConductorServerUtils.failureResponse("Validation errors: " + message,
                                                        uriInfo.getPath().replaceAll("^ui", ""));
        }
        val logCode = UUID.randomUUID().toString();
        return Response.serverError()
                .entity(ConductorApiResponse.failure(
                        ConductorErrorCode.UNHANDLED_SERVER_ERROR,
                        Map.of("message", message,
                               "logCode", logCode),
                        "Unhandled exception occurred.Please mention the log code when reporting this incident."))
                .build();
    }
}
