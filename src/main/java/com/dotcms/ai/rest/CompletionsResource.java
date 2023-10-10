package com.dotcms.ai.rest;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Call
 */
@Path("/v1/ai/completions")
public class CompletionsResource {

    private final WebResource webResource = new WebResource();


    private final Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9 \\-,.()]*$");

    private String sanitizeParams(String in) {
        return (in == null || allowedPattern.matcher(in).matches()) ? in : "";

    }

    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                      @QueryParam("query") String query,
                                      @DefaultValue("1000") @QueryParam("searchLimit") int searchLimit,
                                      @QueryParam("site") String site,
                                      @QueryParam("contentType") String contentType,
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @DefaultValue("1024") @QueryParam("responseLength") int responseLength,
                                      @DefaultValue("cosine") @QueryParam("operator") String operator,
                                      @QueryParam("fieldVar") String fieldVar) throws DotDataException, DotSecurityException, IOException {

        CompletionsForm form = new CompletionsForm.Builder()
                .query(query)
                .searchLimit(searchLimit)
                .site(site)
                .contentType(contentType)
                .fieldVar(fieldVar)
                .threshold(threshold)
                .operator(operator)
                .stream(stream)
                .responseLengthTokens(responseLength)
                .build();

        return searchByPost(request, response, form);
    }

    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       CompletionsForm form

    ) throws DotDataException, DotSecurityException, IOException {

        // get user if we have one (this is allow anon)
        User user = new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.READ).init().getUser();

        if (form.query == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "query required")).build();
        }


        long startTime = System.currentTimeMillis();


        List<Float> queryEmbeddings = EmbeddingsAPI.impl().generateEmbeddingsforString(form.query);


        if (!form.stream) {
            JSONObject jsonResponse = CompletionsAPI.impl().summarize(form);
            JSONObject map = new JSONObject();
            map.put("timeToEmbeddings", System.currentTimeMillis() - startTime + "ms");
            map.put("total", jsonResponse.size());
            map.put("threshold", form.threshold);
            map.put("results", jsonResponse);
            map.put("operator", form.operator);
            map.put("searchLimit", form.searchLimit);

            return Response.ok(map.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {
            CompletionsAPI.impl().summarizeStream(form, output);
            output.flush();
            output.close();

        };

        return Response.ok(streaming).build();
    }

}
