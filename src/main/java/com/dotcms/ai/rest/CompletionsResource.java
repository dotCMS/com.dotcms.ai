package com.dotcms.ai.rest;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.OpenAIModel;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Call
 */
@Path("/v1/ai/completions")
public class CompletionsResource {


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
                                      @DefaultValue("gpt-3.5-turbo-16k") @QueryParam("model") String model,
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @QueryParam("responseLength") int responseLength,
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
                .model(model)
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


        List<Float> queryEmbeddings = EmbeddingsAPI.impl().pullOrGenerateEmbeddings(form.query)._2;


        if (!form.stream) {
            JSONObject jsonResponse = CompletionsAPI.impl().summarize(form);
            jsonResponse.put("totalTime", System.currentTimeMillis() - startTime + "ms");
            return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {
            CompletionsAPI.impl().summarizeStream(form, output);
            output.flush();
            output.close();

        };

        return Response.ok(streaming).build();
    }

    @GET
    @JSONP
    @Path("/config")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response getConfig(@Context final HttpServletRequest request, @Context final HttpServletResponse response) throws DotDataException, DotSecurityException, IOException {
        User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        AppConfig app = ConfigService.INSTANCE.config(host);


        Map<String,String> map = new HashMap<>();
        map.put("configHost", host.getHostname() + " (falls back to system host)");
        for(AppKeys config : AppKeys.values()){
            String key = config.key;

            String value = config==AppKeys.API_KEY ? "******" :  app.getConfig(config);

            map.put(config.key, value);
        }




        return Response.ok(map).build();
    }



}
