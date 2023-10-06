package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Call
 */
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {


    Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9 \\-,.()]*$");

    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {

        ResponseBuilder builder = Response.ok(Map.of("type", "embeddings"), MediaType.APPLICATION_JSON);
        return builder.build();
    }

    @POST
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response embed(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                JSONObject json

    ) throws DotDataException, DotSecurityException {
        // force authentication
        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        final String inode = sanitizeParams(json.optString("inode"));
        final String identifier = sanitizeParams(json.optString("identifier"));
        final long language = json.optLong("language", APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final int limit = json.optInt("limit", 1000);
        final int offset = json.optInt("offset");
        final String fieldVar = json.optString("fieldVar");
        final String indexName = json.optString("indexName", "default");

        String query = json.optString("query") != null
                ? json.optString("query")
                : UtilMethods.isSet(inode)
                ? " +inode:" + inode
                : " +identifier:" + identifier + " +language:" + language;


        long startTime = System.currentTimeMillis();

        if (UtilMethods.isEmpty(query)) {
            return Response.ok("query is required").build();
        }


        // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
        List<ContentletSearch> searchResults = APILocator.getContentletAPI().searchIndex(query, limit, offset, "moddate", user, false);

        int good, bad = 0;
        for (ContentletSearch searcher : searchResults) {
            Contentlet contentlet = APILocator.getContentletAPI().find(searcher.getInode(), user, false);
            Optional<Field> field = contentlet.getContentType().fields().stream().filter(f -> f.variable().equalsIgnoreCase(fieldVar)).findFirst();
            try {
                EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, field, indexName);
            } catch (Exception e) {

            }
        }

        long totalTime = System.currentTimeMillis() - startTime;


        Map<String, Object> map = Map.of("timeToEmbeddings", totalTime + "ms", "totalToEmbed", searchResults.size(), "indexName", indexName);
        ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);

        return builder.build();

    }

    String sanitizeParams(String in) {
        if (in == null || allowedPattern.matcher(in).matches()) {
            return in;
        }
        return "";
    }

    @DELETE
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 JSONObject json) throws DotDataException, DotSecurityException {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIdentifier(json.optString("identifier"))
                .withLanguage(json.optLong("language", 0))
                .withInode(json.optString("inode"))
                .withContentType(json.optString("contentType"))
                .withField(json.optString("fieldVar"))
                .withHost(json.optString("site"))
                .build();
        int deleted = EmbeddingsAPI.impl(null).deleteEmbedding(dto);
        return Response.ok(Map.of("deleted", deleted)).build();

    }

    @DELETE
    @JSONP
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response dropAndRecreateTables(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                                JSONObject json) throws DotDataException, DotSecurityException {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();


        EmbeddingsAPI.impl().dropEmbeddingsTable();
        EmbeddingsAPI.impl().initEmbeddingsTable();
        return Response.ok(Map.of("created", true)).build();

    }
}
