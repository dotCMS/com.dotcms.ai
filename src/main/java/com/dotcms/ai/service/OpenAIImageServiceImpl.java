package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.util.Logger;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.ai.util.StopwordsUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public class OpenAIImageServiceImpl implements OpenAIImageService {


    private final AppConfig config;

    public OpenAIImageServiceImpl(AppConfig appConfig) {
        config = appConfig;
    }

    @Override
    public JSONObject sendRawRequest(String prompt) {
        return sendRequest(new JSONObject(prompt));
    }

    @Override
    public JSONObject sendTextPrompt(String textPrompt) {
        AIImageRequestDTO dto = new AIImageRequestDTO.Builder().prompt(textPrompt).build();
        return sendRequest(dto);
    }

    @Override
    public JSONObject sendRequest(JSONObject jsonObject) {
        if(!jsonObject.containsKey("prompt")){
            throw new DotRuntimeException("Image request missing `prompt` key:" + jsonObject);
        }

        jsonObject.putIfAbsent("model", config.getImageModel());
        jsonObject.putIfAbsent("size", config.getImageSize());
        jsonObject.putIfAbsent("n", 1);

        try {
            String responseString = OpenAIRequest.doRequest(config.getApiImageUrl(), "POST", config.getApiKey(),
                    jsonObject);

            JSONObject returnObject = new JSONObject(responseString).getJSONArray("data").getJSONObject(0);
            returnObject.put("originalPrompt", jsonObject.getString("prompt"));
            returnObject.put("tempFileName", generateFileName(jsonObject.getString("prompt")));


            return createTempFile(returnObject);

        } catch (Exception e) {
            Logger.warn(this.getClass(), "image request failed:" + e.getMessage(), e);
            throw new DotRuntimeException("Error generating image:" + e, e);
        }
    }

    /**
     * @param dto
     * @return
     */
    @Override
    public JSONObject sendRequest(AIImageRequestDTO dto) {

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", config.getImageModel());
        jsonRequest.put("prompt", dto.getPrompt());
        jsonRequest.put("size", dto.getSize());
        jsonRequest.put("n", dto.getNumberOfImages());
        return sendRequest(jsonRequest);


    }

    private JSONObject createTempFile(JSONObject imageResponse) {

        final String url = imageResponse.optString("url");



        if (UtilMethods.isEmpty(() -> url)) {
            Logger.warn(this.getClass(), "imageResponse does not include URL:" + imageResponse.toString());
            throw new DotRuntimeException("Image Response does not include URL:" + imageResponse);
        }

        try {

            final String fileName = generateFileName(imageResponse.getString("originalPrompt"));
            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            DotTempFile file = tempApi.createTempFileFromUrl(fileName, getRequest(),
                    new URL(url), 20, Integer.MAX_VALUE);
            imageResponse.put("response", file.id);
            return imageResponse;

        } catch (Exception e) {

            imageResponse.put("response", e.getMessage());
            imageResponse.put("error", e.getMessage());
            Logger.warn(this.getClass(), "Error building tempfile:" + e.getMessage(), e);
            throw new DotRuntimeException("Error building tempfile from:" + imageResponse);
        }
    }

    HttpServletRequest getRequest() {
        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }

        String hostName = Try.of(
                        () -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname())
                .getOrElse("localhost");
        return new MockSessionRequest(
                new MockHeaderRequest(
                        new FakeHttpRequest(hostName, "/").request(), "referer",
                        "https://" + hostName + "/fakeRefer"
                ).request()
        );


    }

    private String generateFileName(String originalPrompt){
        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyyMMdd_hhmmss");
        try {
            String newFileName = originalPrompt.toLowerCase();
            newFileName = newFileName.replaceAll("[^a-z0-9 -]", "");
            newFileName = new StopwordsUtil().removeStopwords(originalPrompt);

            newFileName = String.join("-", newFileName.split("\\s+"));
            newFileName = newFileName.substring(0, Math.min(200,newFileName.length()));
            return newFileName.substring(0, newFileName.lastIndexOf("-"))
                    + "_"
                    + dateToString.format(new Date())
                    + ".png";
        }
        catch (Exception e){
            Logger.warn(this.getClass(), "unable to generate filename: " + e.getMessage(), e);
            return "temp_" + System.currentTimeMillis() + ".png";

        }

    }






}
