package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Call
 */
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {


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

    ) {
        // force authentication
        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        final int limit = json.optInt("limit", 1000) < 1 ? 1000 : json.optInt("limit", 1000);
        final int offset = json.optInt("offset", 0) < 0 ? 0 : json.optInt("offset", 0);
        final String fieldVar = json.optString("fieldVar");
        final String indexName = json.optString("indexName", "default");
        final String query = getQuery(json);


        long startTime = System.currentTimeMillis();

        if (UtilMethods.isEmpty(query)) {
            return Response.ok("query is required").build();
        }
        try {
            int added = 0;
            int newOffset = offset;
            for (int i = 0; i < 10000; i++) {

                // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
                List<ContentletSearch> searchResults = APILocator.getContentletAPI().searchIndex(query, limit, newOffset, "moddate", user, false);
                if (searchResults.isEmpty()) {
                    break;
                }
                newOffset += limit;


                for (ContentletSearch results : searchResults) {
                    Contentlet contentlet = APILocator.getContentletAPI().find(results.getInode(), user, false);
                    if (UtilMethods.isEmpty(contentlet::getContentType)) {
                        continue;
                    }
                    Optional<Field> field = contentlet.getContentType().fields().stream().filter(f -> fieldVar.equalsIgnoreCase(f.variable())).findFirst();
                    EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, field, indexName);
                    if (++added >= limit) {
                        break;
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;


            Map<String, Object> map = Map.of("timeToEmbeddings", totalTime + "ms", "totalToEmbed", added, "indexName", indexName);
            ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);

            return builder.build();
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }

    }

    String getQuery(JSONObject json) {
        StringBuilder builder = new StringBuilder(json.optString("query", ""));
        if (UtilMethods.isSet(json.optString("inode"))) {
            builder.append(" +inode:").append(json.optString("inode"));
        }
        if (UtilMethods.isSet(json.optString("identifier"))) {
            builder.append(" +identifier:").append(json.optString("identifier"));
        }
        if (UtilMethods.isSet(json.optString("language"))) {
            builder.append(" +language:").append(json.optString("language"));
        }
        return builder.toString();
    }


    @DELETE
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 JSONObject json) {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIndexName(json.optString("indexName"))
                .withIdentifier(json.optString("identifier"))
                .withLanguage(json.optLong("language", 0))
                .withInode(json.optString("inode"))
                .withContentType(json.optString("contentType"))
                .withField(json.optString("fieldVar"))
                .withHost(json.optString("site"))
                .build();
        int deleted = EmbeddingsAPI.impl().deleteEmbedding(dto);
        return Response.ok(Map.of("deleted", deleted)).build();

    }

    @DELETE
    @JSONP
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response dropAndRecreateTables(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                                JSONObject json) {
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

    @GET
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request, @Context final HttpServletResponse response,

                                @QueryParam("site") String site,
                                @QueryParam("contentType") String contentType,
                                @QueryParam("indexName") String indexName,
                                @QueryParam("language") String language,
                                @QueryParam("identifier") String identifier,
                                @QueryParam("inode") String inode,
                                @QueryParam("fieldVar") String fieldVar) {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        CompletionsForm form = new CompletionsForm.Builder().contentType(contentType).site(site).language(language).fieldVar(fieldVar).indexName(indexName).prompt("NOT USED").build();
        return count(request, response, form);


    }

    @POST
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                CompletionsForm form) {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        form = (form == null) ? new CompletionsForm.Builder().prompt("NOT USED").build() : form;
        EmbeddingsDTO dto = EmbeddingsDTO.from(form).build();


        return Response.ok(Map.of("embeddingsCount", EmbeddingsDB.impl.get().countEmbeddings(dto))).build();

    }

    @GET
    @JSONP
    @Path("/indexCount")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexCount(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        return Response.ok(Map.of("indexCount", EmbeddingsDB.impl.get().countEmbeddingsByIndex())).build();

    }
}
