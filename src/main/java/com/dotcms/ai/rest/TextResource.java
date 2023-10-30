package com.dotcms.ai.rest;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIErrorResponseDTO;
import com.dotcms.ai.model.AITextRequestDTO;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotcms.ai.service.ChatGPTTextService;
import com.dotcms.ai.service.ChatGPTTextServiceImpl;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;

@Path("/v1/ai/text")
public class TextResource {

    @Path("/generate")
    @GET
    public Response doGet(@Context HttpServletRequest request,
                          @Context final HttpServletResponse response,
                          @QueryParam("prompt") String prompt) throws IOException {
        return doPost(request, response, new AITextRequestDTO(prompt));
    }

    @Path("/generate")
    @POST
    public Response doPost(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           AITextRequestDTO aiTextRequestDTO) throws IOException {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();


        if (UtilMethods.isEmpty(aiTextRequestDTO.getPrompt())) {
            return createErrorResponse("MissingRequestBody", "Missing request body.", Status.BAD_REQUEST);
        } else {
            return handleTextRequest(request, aiTextRequestDTO);
        }


    }

    private Response createErrorResponse(String errorCode, String errorMessage, Status status) throws IOException {
        AIErrorResponseDTO errorResponse = new AIErrorResponseDTO(errorCode, errorMessage);
        return Response.status(status)
                .entity(Marshaller.marshal(errorResponse))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    /**
     * Logs incoming request to plugin API. First checks if config is available, and returns error if it is not or prompt is empty.
     * Than it calls TextService
     *
     * @param request
     * @param aiTextRequestDTO
     * @return
     * @throws IOException
     */
    private Response handleTextRequest(HttpServletRequest request, AITextRequestDTO aiTextRequestDTO) throws IOException {
        Logger.debug(this.getClass(), String.format("[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
                request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(), readParameters(request.getParameterMap()), "POST".equals(request.getMethod()) ? Marshaller.marshal(aiTextRequestDTO) : ""));

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));


        if (StringUtils.isBlank(aiTextRequestDTO.getPrompt())) {
            return createErrorResponse("MissingParameter", "The 'prompt' is required.", Status.BAD_REQUEST);
        }

        ChatGPTTextService service = new ChatGPTTextServiceImpl(config);
        AITextResponseDTO resp = service.sendChatGPTRequest(aiTextRequestDTO.getPrompt(), false);

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
}
