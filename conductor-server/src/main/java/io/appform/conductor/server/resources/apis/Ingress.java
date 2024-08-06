package io.appform.conductor.server.resources.apis;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/ingress")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Ingress {

    private final TicketManager ticketManager;

    @POST
    @Path("/raw/data")
    public Response rawProcessing(@NotNull JsonNode payload) {
        return ticketManager.processRaw(payload)
                        .map(ticketDetails -> Response.accepted().build())
                        .orElse(Response.notModified().build());

    }


    @POST
    @Path("/callback/{translatorId}/{ticketId}")
    public Response rawProcessing(@NotEmpty @PathParam("translatorId") String translatorId,
                                  @NotEmpty @PathParam("ticketId") String ticketId,
                                  @NotNull JsonNode payload) {
        return  ticketManager.processCallback(translatorId, ticketId, payload)
                        .map(ticketDetails -> Response.accepted().build())
                        .orElse(Response.notModified().build());

    }

    @POST
    @Path("/callback/obd/{translatorId}")
    public Response obdCallbackProcessing(@NotEmpty @PathParam("translatorId") String translatorId,
                                  @NotNull JsonNode payload) {
        return  ticketManager.processCallback(translatorId, payload)
                .map(ticketDetails -> Response.accepted().build())
                .orElse(Response.notModified().build());

    }
}
