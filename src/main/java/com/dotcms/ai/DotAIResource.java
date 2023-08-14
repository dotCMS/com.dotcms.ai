package com.dotcms.ai;

import com.dotcms.ai.model.AITextRequestDTO;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIErrorResponseDTO;
import com.dotcms.ai.model.AITextResponseDTO;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import java.io.IOException;

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

@Path("/ai/text/generate")
public class DotAIResource {

    @POST
    public Response doPost(@Context HttpServletRequest request) throws IOException {
        Response response;
        if (request.getInputStream().available() == 0) {
            response = createErrorResponse("MissingRequestBody", "Missing request body.", Status.BAD_REQUEST);
        } else {
            AITextRequestDTO aiTextRequestDTO = Marshaller.unmarshal(request.getInputStream(), AITextRequestDTO.class);
            response = handleRequest(request, aiTextRequestDTO);
        }

        Logger.info(this.getClass(), String.format("[DotAI API response] : HTTPStatusCode = %s, responseBody = %s", response.getStatus(), response.getEntity().toString()));
        return response;
    }


    @GET
    public Response doGet(@Context HttpServletRequest request, @QueryParam("prompt") String prompt) throws IOException {
        Response response;
        if (StringUtils.isBlank(prompt)) {
            response = createErrorResponse("MissingParameter", "The 'prompt' parameter is required.", Status.BAD_REQUEST);
        } else {
            AITextRequestDTO aiTextRequestDTO = new AITextRequestDTO(prompt);
            response = handleRequest(request, aiTextRequestDTO);
        }

        Logger.info(this.getClass(), String.format("[DotAI API response] : HTTPStatusCode = %s, responseBody = %s", response.getStatus(), response.getEntity().toString()));
        return response;

    }

    private Response handleRequest(HttpServletRequest request, AITextRequestDTO aiTextRequestDTO) throws IOException {
        Logger.info(this.getClass(), String.format("[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
            request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(), readParameters(request.getParameterMap()), "POST".equals(request.getMethod()) ? Marshaller.marshal(aiTextRequestDTO) : ""));

        final Optional<AppConfig> config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        if (!config.isPresent()) {
            return createErrorResponse("ConfigMissing", "App Config missing", Status.INTERNAL_SERVER_ERROR);
        }

        if (StringUtils.isBlank(aiTextRequestDTO.getPrompt())) {
            return createErrorResponse("MissingParameter", "The 'prompt' is required.", Status.BAD_REQUEST);
        }

        ChatGPTService service = new ChatGPTServiceImpl(config.get());
        AITextResponseDTO resp = service.sendChatGPTRequest(aiTextRequestDTO.getPrompt(), config, false);

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