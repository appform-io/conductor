package io.appform.conductor.server.errorhandlers;

import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.server.utils.ConductorServerUtils;
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
public class CatchAllExceptionHandler implements ExceptionMapper<Exception> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        val message = ConductorServerUtils.errorMessage(exception);
        val logCode = UUID.randomUUID().toString();
        if (uriInfo != null && uriInfo.getPath().startsWith("ui/")) {
            return ConductorServerUtils.failureResponse("Exception: log code: " + logCode
                                                                + " Message: " + exception.getMessage(),
                                                        uriInfo.getPath().replaceAll("^ui", ""));
        }
        log.error("Exception occurred. Log code: " + logCode + " Message: " + exception.getMessage(), exception);
        final var context = Map.<String, Object>of("message", message,
                                    "logCode", logCode);
        return Response.serverError()
                .entity(ConductorApiResponse.failure(
                                ConductorErrorCode.UNHANDLED_SERVER_ERROR,
                                context,
                                ConductorException.generateErrorMessage(ConductorErrorCode.UNHANDLED_SERVER_ERROR,
                                                                        context,
                                                                        exception)))
                .build();
    }
}
