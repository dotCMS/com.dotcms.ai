package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.knuddels.jtokkit.api.Encoding;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.Map;
import java.util.Optional;

public class SummarizeAPIImpl implements SummarizeAPI {


    final Lazy<AppConfig> config;

    final Lazy<AppConfig> defaultConfig = Lazy.of(() -> Try.of(() -> ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest())).get()).getOrElseThrow(e -> new DotRuntimeException(e)));




    public SummarizeAPIImpl(Lazy<AppConfig> config) {

        this.config = (config != null)
                ? config
                : defaultConfig;
    }


    public SummarizeAPIImpl() {
        this(null);
    }


    private String getTextPrompt(String query, String supportingText) {
        if (UtilMethods.isEmpty(query) || UtilMethods.isEmpty(supportingText)) {
            throw new DotRuntimeException("no query or supportingText to summarize found");

        }
        String  textPrompt = config.get().getSearchTextPrompt();


        return textPrompt.replace("{query}", query).replace("{supportingText}", supportingText);

    }

    private int countTokens(String testString) {
        Optional<Encoding> encoderOpt = EncodingUtil.registry.getEncodingForModel(config.get().getModel());
        if (encoderOpt.isEmpty()) {
            throw new DotRuntimeException("Encoder not found");
        }

        return encoderOpt.get().countTokens(testString);

    }

    final Map<String, Integer> maxTokensPerModel = Map.of("gpt-4", 8192, "gpt-4-32k", 32768, "gpt-4-32k-0613", 32768, "gpt-3.5-turbo", 4096, "gpt-3.5-turbo-16k", 16384, "gpt-3.5-turbo-0613", 4096, "gpt-3.5-turbo-16k-0613", 16384, "text-davinci-003", 4097, "text-davinci-002", 4097, "code-davinci-002", 8001);

    /**
     * chat gpt allow 4097 total tokens,including the prompts and the 1024 they return;
     * @param incoming
     * @param maxTokenSize
     * @return
     */
    private String reduceStringToTokenSize(String incoming, int maxTokenSize) {

        if (maxTokenSize <= 0) {
            throw new DotRuntimeException("maxToken size must be greater than 0");
        }

        int tokenCount = countTokens(incoming);

        if (tokenCount <= maxTokenSize) {
            return incoming;
        }

        String[] wordsToKeep = incoming.split("\\s+");
        String textToKeep = null;
        for (int i = 0; i < 10000; i++) {
            // float percentage = maxTokenSize / (float) tokenCount;
            // decrease by 10%
            int toRemove = Math.round(wordsToKeep.length * .1f);

            wordsToKeep = ArrayUtils.subarray(wordsToKeep, 0, wordsToKeep.length - toRemove);
            textToKeep = String.join(" ", wordsToKeep);
            tokenCount = countTokens(textToKeep);
            if (tokenCount < maxTokenSize) {
                break;
            }
        }

        return textToKeep;

    }





    private JSONObject generateRequestObject(String query, String supportingText, int responseLengthTokens){
        int maxTokenSize = maxTokensPerModel.getOrDefault(config.get().getModel(), 4096);
        String systemPrompt = config.get().getSearchSystemPrompt();


        String textPrompt = getTextPrompt(query, supportingText);

        int systemPromptTokens = countTokens(systemPrompt);


        textPrompt = reduceStringToTokenSize(textPrompt, maxTokenSize - responseLengthTokens - systemPromptTokens);
        JSONArray messages = new JSONArray();

        messages.add(Map.of("role", "user", "content", textPrompt));
        messages.add(Map.of("role", "system", "content", systemPrompt));
        JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.put("model", config.get().getModel());
        json.put("temperature", 0);
        json.put("max_tokens", responseLengthTokens);

        json.put("stream", false);

        return json;
    }




    @Override
    public JSONObject summarize(String query, String docText, int responseLengthTokens) {

        JSONObject json = generateRequestObject(query, docText, responseLengthTokens);


        String responseString = Try.of(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json.toString())).getOrElseThrow(e -> new DotRuntimeException(e));
        return new JSONObject(responseString);


    }
    @Override
    public void summarizeStream(String query, String docText, int responseLengthTokens, OutputStream out) {
        JSONObject json = generateRequestObject(query, docText, responseLengthTokens);
        json.put("stream", true);
        Try.run(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json.toString(), out)).getOrElseThrow(e -> new DotRuntimeException(e));


    }

}
