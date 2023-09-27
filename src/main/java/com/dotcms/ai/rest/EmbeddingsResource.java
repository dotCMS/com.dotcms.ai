package com.dotcms.ai.rest;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Call
 */
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {

    private final WebResource webResource = new WebResource();

    @Context
    private HttpServletRequest httpRequest;


    @GET
    @JSONP
    @Path("/index")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response test(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @DefaultValue("60") @QueryParam("contentInode") String contentInode) throws DotDataException, DotSecurityException {
        // force authentication
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init();

        Map<String, String> map = new HashMap<>();

        Contentlet contentlet = APILocator.getContentletAPI().find(contentInode, APILocator.systemUser(),true);







        ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);


        return builder.build();

    }






}
