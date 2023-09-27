package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Call
 */
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {

    private final WebResource webResource = new WebResource();


    Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9 \\-,.()]*$");

    String sanitizeParams(String in) {
        if(in ==null){
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
    public final Response indexByInode(@Context final HttpServletRequest request, @Context final HttpServletResponse response )  {

        ResponseBuilder builder = Response.ok(Map.of("type", "embeddings"), MediaType.APPLICATION_JSON);
        return builder.build();
    }


    @POST
    @JSONP
    @Path("/embed")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       JSONObject json


    ) throws DotDataException, DotSecurityException {
        String inode = json.optString("inode", null);
        String identifier = json.optString("identifier", null);
        long language = json.optLong("language", 0);
        inode = sanitizeParams(inode);
        identifier = sanitizeParams(identifier);
        language = language == 0 ? APILocator.getLanguageAPI().getDefaultLanguage().getId() : language;

        String query = UtilMethods.isSet(inode)
                ? " +inode:" + inode
                : " +identifier:" + identifier + " +language:" + language;

        JSONObject jsonOut = new JSONObject();
        jsonOut.put("query", query);





        return indexByQuery(request,response, jsonOut);

    }




    @POST
    @JSONP
    @Path("/embedByQuery")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByQuery(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       JSONObject json

                                       ) throws DotDataException, DotSecurityException {
        // force authentication
        User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();


        String query = json.optString("query", null);
        int limit = json.optInt("limit", 1000);
        int offset = json.optInt("offset", 0);
        String fieldVar = json.optString("fieldVar", null);
        long startTime = System.currentTimeMillis();


        // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
        List<ContentletSearch> searchResults = APILocator.getContentletAPI().searchIndex(query, limit, offset, "mod_date", user, false);


        for (ContentletSearch searcher : searchResults) {
            Contentlet contentlet = APILocator.getContentletAPI().find(searcher.getInode(), user, false);
            Optional<Field> field = contentlet.getContentType().fields().stream().filter(f -> f.variable().equalsIgnoreCase(fieldVar)).findFirst();
            EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, field);
        }

        long totalTime = System.currentTimeMillis() - startTime;


        Map<String, Object> map = Map.of("timeToEmbeddings", totalTime + "ms", "totalIndexed", searchResults.size());
        ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);

        return builder.build();

    }

    @POST
    @JSONP
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchByPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       JSONObject json

                                       ) throws DotDataException, DotSecurityException {
        String query = json.optString("query", null);
        int limit = json.optInt("limit", 1000);
        int offset = json.optInt("limit", 0);
        String fieldVar = json.optString("fieldVar", null);
        String contentType = json.optString("contentType", null);

        long startTime = System.currentTimeMillis();
        // force authentication
        User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();


        List<Double> queryEmbeddings = EmbeddingsAPI.impl().generateEmbeddingsforString(query);
        EmbeddingsDTO dto = new EmbeddingsDTO.Builder().withEmbeddings(queryEmbeddings).withField(fieldVar).withContentType(contentType).build();

        List<EmbeddingsDTO> searchResults = EmbeddingsDB.impl.get().searchEmbeddings(dto);

        // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
        //List<ContentletSearch> searchResults = APILocator.getContentletAPI().searchIndex(query, limit, offset, "mod_date", user, false);

        List<Contentlet> results = new ArrayList<>();
        for (EmbeddingsDTO searcher : searchResults) {
            results.add(APILocator.getContentletAPI().find(searcher.inode, user, false));
        }

        long totalTime = System.currentTimeMillis() - startTime;


        Map<String, Object> map = Map.of("timeToEmbeddings", totalTime + "ms", "totalIndexed", searchResults.size(), "results", results);

        ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);


        return builder.build();

    }

    @GET
    @JSONP
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchByGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @QueryParam("query") String query, @DefaultValue("10") @QueryParam("limit") int limit, @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("%") @QueryParam("contentType") String contentType, @DefaultValue("%") @QueryParam("fieldVar") String fieldVar) throws DotDataException, DotSecurityException {
        JSONObject json = new JSONObject();
        json.put("query", query);
        json.put("limit", limit);
        json.put("offest", offset);
        json.put("contentType", contentType);
        json.put("fieldVar", fieldVar);


        return searchByPost(request, response, json);
    }


}
