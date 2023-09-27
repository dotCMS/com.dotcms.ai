package com.dotcms.ai.rest;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.model.AITextRequestDTO;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIErrorResponseDTO;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.service.ChatGPTImageService;
import com.dotcms.ai.service.ChatGPTImageServiceImpl;
import com.dotcms.ai.service.ChatGPTTextService;
import com.dotcms.ai.service.ChatGPTTextServiceImpl;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.StringUtils;

@Path("/ai/")
public class DotAIResource {

    @Path("text/generate")
    @POST
    public Response doPost(@Context HttpServletRequest request) throws IOException {
        Response response;
        if (request.getInputStream().available() == 0) {
            response = createErrorResponse("MissingRequestBody", "Missing request body.", Status.BAD_REQUEST);
        } else {
            AITextRequestDTO aiTextRequestDTO = Marshaller.unmarshal(request.getInputStream(), AITextRequestDTO.class);
            response = handleTextRequest(request, aiTextRequestDTO);
        }

        Logger.debug(this.getClass(), String.format("[DotAI API response] : HTTPStatusCode = %s, responseBody = %s", response.getStatus(), response.getEntity().toString()));
        return response;
    }


    @Path("text/generate")
    @GET
    public Response doGet(@Context HttpServletRequest request, @QueryParam("prompt") String prompt) throws IOException {
        Response response;
        if (StringUtils.isBlank(prompt)) {
            response = createErrorResponse("MissingParameter", "The 'prompt' parameter is required.", Status.BAD_REQUEST);
        } else {
            AITextRequestDTO aiTextRequestDTO = new AITextRequestDTO(prompt);
            response = handleTextRequest(request, aiTextRequestDTO);
        }

        Logger.debug(this.getClass(), String.format("[DotAI API response] : HTTPStatusCode = %s, responseBody = %s", response.getStatus(), response.getEntity().toString()));
        return response;

    }

    @Path("image/generate")
    @POST
    public Response doPostImage(@Context HttpServletRequest request) throws IOException {
        User user = (User) request.getAttribute("USER");
        Response response;
        if (request.getInputStream().available() == 0) {
            response = createErrorResponse("MissingRequestBody", "Missing request body.", Status.BAD_REQUEST);
        } else {
            AIImageRequestDTO aiImageRequestDTO = Marshaller.unmarshal(request.getInputStream(), AIImageRequestDTO.class);
            response = handleImageRequest(request, aiImageRequestDTO);
        }

        Logger.debug(this.getClass(), String.format("[DotAI API response] : HTTPStatusCode = %s, responseBody = %s", response.getStatus(), response.getEntity().toString()));
        return response;
    }

    /**
     * Logs incoming request to plugin API. First checks if config is available, and returns error if it is not or prompt is empty.
     * Than it calls TextService
     * @param request
     * @param aiTextRequestDTO
     * @return
     * @throws IOException
     */
    private Response handleTextRequest(HttpServletRequest request, AITextRequestDTO aiTextRequestDTO) throws IOException {
        Logger.debug(this.getClass(), String.format("[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
            request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(), readParameters(request.getParameterMap()), "POST".equals(request.getMethod()) ? Marshaller.marshal(aiTextRequestDTO) : ""));

        final Optional<AppConfig> config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        if (!config.isPresent()) {
            return createErrorResponse("ConfigMissing", "App Config missing", Status.INTERNAL_SERVER_ERROR);
        }

        if (StringUtils.isBlank(aiTextRequestDTO.getPrompt())) {
            return createErrorResponse("MissingParameter", "The 'prompt' is required.", Status.BAD_REQUEST);
        }

        ChatGPTTextService service = new ChatGPTTextServiceImpl(config.get());
        AITextResponseDTO resp = service.sendChatGPTRequest(aiTextRequestDTO.getPrompt(), config, false);

        return Response.ok(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();

    }

    /**
     * Logs incoming request to plugin API. First checks if config is available, and returns error if it is not or prompt is empty.
     * Then it calls ImageService. If response is OK creates temp file and adds its name in response
     * @param request
     * @param aiImageRequestDTO
     * @return
     * @throws IOException
     */
    private Response handleImageRequest(HttpServletRequest request, AIImageRequestDTO aiImageRequestDTO) throws IOException {
        Logger.debug(this.getClass(), String.format("[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
            request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(), readParameters(request.getParameterMap()), Marshaller.marshal(aiImageRequestDTO)));

        final Optional<AppConfig> config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        if (!config.isPresent()) {
            return createErrorResponse("ConfigMissing", "App Config missing", Status.INTERNAL_SERVER_ERROR);
        }

        if (StringUtils.isBlank(aiImageRequestDTO.getPrompt())) {
            return createErrorResponse("MissingParameter", "The 'prompt' is required.", Status.BAD_REQUEST);
        }

        ChatGPTImageService service = new ChatGPTImageServiceImpl(config.get());
        AIImageResponseDTO resp = service.sendChatGPTRequest(aiImageRequestDTO.getPrompt(), config, false);

        if (resp.getHttpStatus().equals(String.valueOf(HttpResponseStatus.OK.code()))) {
            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            DotTempFile file;
            try {
                //TODO - remove this part once issue with getting user is resolved
                // user is needed in request, as code will break when creating temp file
                // as it needs user to create folder
                User user = new User("admin");
                request.setAttribute("USER", user);

                file = tempApi.createTempFileFromUrl("ChatGPTImage", request, new URL(resp.getResponse()), 10, 1000);
                resp.setResponse(file.id);
            } catch (DotSecurityException e) {
                resp.setResponse("Unable to create temp file. Error: " + e.getMessage());
                 return Response.status(500).entity(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
        }

        return Response.ok(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();

    }

    private Response createErrorResponse(String errorCode, String errorMessage, Status status) throws IOException {
        AIErrorResponseDTO errorResponse = new AIErrorResponseDTO(errorCode, errorMessage);
        return Response.status(status)
            .entity(Marshaller.marshal(errorResponse))
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
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
}
