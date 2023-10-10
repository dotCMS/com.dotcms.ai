package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
import java.io.IOException;

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


        CompletionsForm form = new CompletionsForm.Builder()
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
                                       CompletionsForm form

    ) throws DotDataException, DotSecurityException, IOException {

        User user = new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.READ).init().getUser();

        EmbeddingsDTO searcher = EmbeddingsDTO.from(form).withUser(user).build();


        return Response.ok(EmbeddingsAPI.impl().searchEmbedding(searcher).toString(), MediaType.APPLICATION_JSON).build();


    }

}
