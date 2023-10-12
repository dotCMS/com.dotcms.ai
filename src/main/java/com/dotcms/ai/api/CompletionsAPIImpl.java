package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIModel;
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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CompletionsAPIImpl implements CompletionsAPI {


    final Lazy<AppConfig> config;

    final Lazy<AppConfig> defaultConfig = Lazy.of(() -> Try.of(() -> ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))).getOrElseThrow(DotRuntimeException::new));



    public CompletionsAPIImpl() {
        this(null);
    }


    public CompletionsAPIImpl(Lazy<AppConfig> config) {

        this.config = (config != null)
                ? config
                : defaultConfig;
    }

    @Override
    public JSONObject summarize(CompletionsForm summaryRequest) {
        EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest)
                .build();
        List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);

        // send all this as a json blob to OpenAI
        JSONObject json = buildRequestDataJson(summaryRequest, localResults);


        String responseString = Try.of(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json)).getOrElseThrow(DotRuntimeException::new);
        return new JSONObject(responseString);


    }

    private JSONObject buildRequestDataJson(CompletionsForm form, List<EmbeddingsDTO> searchResults) {


        int maxTokenSize = OpenAIModel.resolveModel(config.get().getModel()).maxTokens;
        // aggregate matching results into text
        StringBuilder supportingText = new StringBuilder();
        searchResults.forEach(s -> supportingText.append(s.extractedText + " "));


        String systemPrompt = getSystemPrompt(form.query, supportingText.toString());
        String textPrompt = getTextPrompt(form.query, supportingText.toString());

        int systemPromptTokens = countTokens(systemPrompt);

        JSONArray messages = new JSONArray();
        textPrompt = reduceStringToTokenSize(textPrompt, maxTokenSize - form.responseLengthTokens - systemPromptTokens);

        messages.add(Map.of("role", "user", "content", textPrompt));
        messages.add(Map.of("role", "system", "content", systemPrompt));
        JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.put("model", config.get().getModel());
        json.put("temperature", 0);
        json.put("max_tokens", form.responseLengthTokens);

        json.put("stream", false);

        return json;
    }

    private String getSystemPrompt(String query, String supportingText) {
        if (UtilMethods.isEmpty(query) || UtilMethods.isEmpty(supportingText)) {
            throw new DotRuntimeException("no query or supportingText to summarize found");

        }
        String systemPrompt = config.get().getConfig(AppKeys.COMPLETION_ROLE_PROMPT, "You concisely answer questions based on text that is provided to you.");


        return systemPrompt.replace("${query}", query).replace("${supportingText}", supportingText).replace("??", "?");

    }

    private String getTextPrompt(String query, String supportingText) {
        if (UtilMethods.isEmpty(query) || UtilMethods.isEmpty(supportingText)) {
            throw new DotRuntimeException("no query or supportingText to summarize found");

        }
        String textPrompt  = config.get().getConfig(AppKeys.COMPLETION_TEXT_PROMPT, "Answer this question\\n\\\"${query}?\\\"\\n\\nby summarizing the following text:\\n\\n${supportingText}");


        return textPrompt.replace("${query}", query).replace("${supportingText}", supportingText).replace("??", "?");

    }

    private int countTokens(String testString) {
        Optional<Encoding> encoderOpt = EncodingUtil.registry.getEncodingForModel(config.get().getModel());
        if (encoderOpt.isEmpty()) {
            throw new DotRuntimeException("Encoder not found");
        }

        return encoderOpt.get().countTokens(testString);

    }

    /***
     * Reduce prompt to fit the maxTokenSize of the model
     * @param incomingString the String to be reduced
     * @param maxTokenSize
     * @return
     */
    private String reduceStringToTokenSize(String incomingString, int maxTokenSize) {

        if (maxTokenSize <= 0) {
            throw new DotRuntimeException("maxToken size must be greater than 0");
        }

        int tokenCount = countTokens(incomingString);

        if (tokenCount <= maxTokenSize) {
            return incomingString;
        }

        String[] wordsToKeep = incomingString.trim().split("\\s+");
        String textToKeep = null;
        for (int i = 0; i < 10000; i++) {
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

    @Override
    public void summarizeStream(CompletionsForm summaryRequest, OutputStream out) {
        EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest)
                .build();

        List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);


        JSONObject json = buildRequestDataJson(summaryRequest, localResults);
        json.put("stream", true);
        Try.run(() -> OpenAIRequest.doPost(config.get().getApiUrl(), config.get().getApiKey(), json, out)).getOrElseThrow(DotRuntimeException::new);


    }

}
