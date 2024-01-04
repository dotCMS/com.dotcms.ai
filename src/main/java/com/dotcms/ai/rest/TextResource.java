package com.dotcms.ai.rest;

import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/v1/ai/text")
public class TextResource {

    @Path("/generate")
    @GET
    public Response doGet(@Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("prompt") String prompt) throws IOException {

        return doPost(request, response, new CompletionsForm.Builder().prompt(prompt).build());
    }

    @Path("/generate")
    @POST
    public Response doPost(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            CompletionsForm formIn) throws IOException {

        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();

        if (UtilMethods.isEmpty(formIn.prompt)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "`prompt` is required"))
                    .build();
        }

        return new CompletionsResource().rawPrompt(request, response, formIn);

    }

}
