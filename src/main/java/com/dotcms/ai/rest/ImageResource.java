package com.dotcms.ai.rest;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIErrorResponseDTO;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.service.ChatGPTImageService;
import com.dotcms.ai.service.ChatGPTImageServiceImpl;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URL;
import java.util.Map;


@Path("/v1/ai/image")
public class ImageResource {


    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response) {

        Response.ResponseBuilder builder = Response.ok(Map.of("type", "image"), MediaType.APPLICATION_JSON);
        return builder.build();
    }

    @GET
    @JSONP
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @QueryParam("prompt") String prompt) throws IOException {
        AIImageRequestDTO dto = new AIImageRequestDTO();
        dto.setPrompt(prompt);
        return handleImageRequest(request, response, dto);
    }

    /**
     * Logs incoming request to plugin API. First checks if config is available, and returns error if it is not or prompt is empty.
     * Then it calls ImageService. If response is OK creates temp file and adds its name in response
     *
     * @param request
     * @param aiImageRequestDTO
     * @return
     * @throws IOException
     */
    @POST
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleImageRequest(@Context HttpServletRequest request,
                                       @Context HttpServletResponse response,
                                       AIImageRequestDTO aiImageRequestDTO) throws IOException {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();


        Logger.debug(this.getClass(), String.format("[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
                request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(), readParameters(request.getParameterMap()), Marshaller.marshal(aiImageRequestDTO)));

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        if (UtilMethods.isEmpty(config.getApiKey())) {
            return createErrorResponse("ConfigMissing", "App Config missing", Status.INTERNAL_SERVER_ERROR);
        }

        if (StringUtils.isBlank(aiImageRequestDTO.getPrompt())) {
            return createErrorResponse("MissingParameter", "The 'prompt' is required.", Status.BAD_REQUEST);
        }

        ChatGPTImageService service = new ChatGPTImageServiceImpl(config);
        AIImageResponseDTO resp = service.sendChatGPTRequest(aiImageRequestDTO.getPrompt(), config, false);

        if (resp.getHttpStatus().equals(String.valueOf(HttpResponseStatus.OK.code()))) {
            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            DotTempFile file;
            try {


                file = tempApi.createTempFileFromUrl("ChatGPTImage", request, new URL(resp.getResponse()), 10, 1000);
                resp.setResponse(file.id);
            } catch (DotSecurityException e) {
                resp.setResponse("Unable to create temp file. Error: " + e.getMessage());
                return Response.status(500).entity(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
        }

        return Response.ok(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();

    }

    private String readParameters(Map<String, String[]> parameterMap) {

        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();

            sb.append(paramName).append(":");

            String[] paramValues = entry.getValue();
            for (String paramValue : paramValues) {
                sb.append(paramValue);
            }
        }
        sb.append("]");

        return sb.toString();
    }

    private Response createErrorResponse(String errorCode, String errorMessage, Status status) throws IOException {
        AIErrorResponseDTO errorResponse = new AIErrorResponseDTO(errorCode, errorMessage);
        return Response.status(status)
                .entity(Marshaller.marshal(errorResponse))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
