package io.appform.conductor.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v1/ingress")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Ingress {

    private final TicketManager ticketManager;

    @POST
    @Path("/raw/data")
    public Response rawProcessing(@NotNull JsonNode payload) {
        Optional<TicketDetails> ticketDetails = ticketManager.processRaw(payload);
        return ticketDetails.isPresent() ?
                Response.accepted().build() :
                Response.notModified().build();

    }


    @POST
    @Path("/callback/{ticketId}")
    public Response rawProcessing(@NotNull @PathParam("ticketId") String ticketId,
                                  @NotNull JsonNode payload) {
        Optional<TicketDetails> ticketDetails = ticketManager.processCallback(ticketId, payload);
        return ticketDetails.isPresent() ?
                Response.ok().build() :
                Response.notModified().build();

    }
}
