package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.api.SummarizeAPI;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Call
 */
@Path("/v1/ai/summarize")
public class SummarizeResource {

    private final WebResource webResource = new WebResource();


    Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9 \\-,.()]*$");

    String sanitizeParams(String in) {
        if (in == null) {
            return in;
        }
        if (allowedPattern.matcher(in).matches()) {
            return in;
        }
        return "";
    }


    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {

        ResponseBuilder builder = Response.ok(Map.of("type", "summarize"), MediaType.APPLICATION_JSON);
        return builder.build();
    }


    @POST
    @JSONP
    @Path("/query")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       JSONObject json

    ) throws DotDataException, DotSecurityException, IOException {

        // get user if we have one (this is allow anon)
        new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.READ).init();
        String query = json.optString("query", json.optString("q", null));
        int searchLimit = json.optInt("limit", 3);
        int responseLength = json.optInt("responseLength", 1024);
        boolean stream = json.optBoolean("stream", false);
        String fieldVar = json.optString("fieldVar", null);
        String contentType = json.optString("contentType", null);
        String operator = json.optString("operator", "<=>");
        String site = UtilMethods.isSet(json.optString("site"))
                ? json.optString("site")
                : WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request).getIdentifier();

        long startTime = System.currentTimeMillis();


        List<Float> queryEmbeddings = EmbeddingsAPI.impl().generateEmbeddingsforString(query);

        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder().withEmbeddings(queryEmbeddings)
                .withField(fieldVar)
                .withContentType(contentType)
                .withHost(site)
                .withLimit(searchLimit)
                .withThreshold((float) json.optDouble("threshold", .8d))
                .withOperator(operator)
                .build();

        List<EmbeddingsDTO> searchResults = EmbeddingsDB.impl.get().searchEmbeddings(searcher);

        StringBuilder promptContext = new StringBuilder();
        searchResults.forEach(s -> promptContext.append(s.extractedText + " "));


        List<Contentlet> supporting



        if(!stream) {
            JSONObject jsonResponse = SummarizeAPI.impl().summarize(query, promptContext.toString(), responseLength);
            JSONObject map = new JSONObject();
            map.put("timeToEmbeddings", System.currentTimeMillis() - startTime + "ms");
            map.put("total", searchResults.size());
            map.put("threshold", searcher.threshold);
            map.put("results", jsonResponse);
            map.put("operator", searcher.operator);
            map.put("limit", searcher.limit);

            return Response.ok(map.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {

            SummarizeAPI.impl().summarizeStream(query, promptContext.toString(), responseLength, output);
            output.flush();
            output.close();

        };

        return Response.ok(streaming).build();
    }






    @GET
    @JSONP
    @Path("/query")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                      @QueryParam("query") String query,
                                      @DefaultValue("3") @QueryParam("limit") int limit,
                                      @DefaultValue("0") @QueryParam("offset") int offset,
                                      @QueryParam("site") String site,
                                      @QueryParam("contentType") String contentType,
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @DefaultValue("1024") @QueryParam("responseLength") int responseLength,
                                      @DefaultValue("<=>>") @QueryParam("operator") String operator,
                                      @QueryParam("fieldVar") String fieldVar) throws DotDataException, DotSecurityException, IOException {
        JSONObject json = new JSONObject();
        json.put("query", query);
        json.put("limit", limit);
        json.put("site", site);
        json.put("offset", offset);
        json.put("contentType", contentType);
        json.put("fieldVar", fieldVar);
        json.put("threshold", threshold);
        json.put("operator", operator);
        json.put("stream", stream);
        json.put("responseLength", responseLength);
        return searchByPost(request, response, json);
    }

}
