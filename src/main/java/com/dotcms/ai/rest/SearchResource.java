package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.SummarizeForm;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ContentResource;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Call
 */
@Path("/v1/ai/search")
public class SearchResource {


    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                      @QueryParam("query") String query,
                                      @DefaultValue("1000") @QueryParam("searchLimit") int searchLimit,
                                      @DefaultValue("0") @QueryParam("searchOffset") int searchOffset,
                                      @QueryParam("site") String site,
                                      @QueryParam("contentType") String contentType,
                                      @DefaultValue("default") @QueryParam("indexName") String indexName,
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @DefaultValue("1024") @QueryParam("responseLength") int responseLength,
                                      @DefaultValue("<=>") @QueryParam("operator") String operator,
                                      @QueryParam("language") String language,
                                      @QueryParam("fieldVar") String fieldVar) throws DotDataException, DotSecurityException, IOException {




        SummarizeForm form = new SummarizeForm.Builder()
                .query(query)
                .searchLimit(searchLimit)
                .site(site)
                .language(language)
                .contentType(contentType)
                .searchOffset(searchOffset)
                .fieldVar(fieldVar)
                .threshold(threshold)
                .indexName(indexName)
                .operator(operator)
                .stream(stream)
                .responseLengthTokens(responseLength)
                .build();

        return searchByPost(request, response, form);
    }

    @POST
    @JSONP

    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchByPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       SummarizeForm form

    ) throws DotDataException, DotSecurityException, IOException {

        User user = new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.READ).init().getUser();

        EmbeddingsDTO searcher = EmbeddingsDTO.from(form).build();


        List<EmbeddingsDTO> searchResults = EmbeddingsAPI.impl().searchEmbedding(searcher);

        Map<String,Object> reducedResults = new LinkedHashMap<>();



        long startTime = System.currentTimeMillis();

        for (EmbeddingsDTO result : searchResults) {

            JSONObject contentObject = (JSONObject) reducedResults.getOrDefault(result.inode,ContentResource.contentletToJSON(APILocator.getContentletAPI().find(result.inode, user, true), request, response, "false", user, false));
            JSONArray matches = contentObject.optJSONArray("matches")==null ? new JSONArray() : contentObject.optJSONArray("matches");
                JSONObject match = new JSONObject();
                match.put("distance", result.threshold);
                match.put("extractedText", result.extractedText);
                matches.add(match);
            contentObject.put("matches", matches);

            if(!reducedResults.containsKey(result.inode)) {
                reducedResults.put(result.inode,contentObject);
            }

        }


        long totalTime = System.currentTimeMillis() - startTime;


        JSONObject map = new JSONObject();
        map.put("timeToEmbeddings", totalTime + "ms");
        map.put("total", searchResults.size());
        map.put("threshold", searcher.threshold);
        map.put("results", reducedResults.values());
        map.put("operator", searcher.operator);


        return Response.ok(map.toString(), MediaType.APPLICATION_JSON).build();


    }

}
