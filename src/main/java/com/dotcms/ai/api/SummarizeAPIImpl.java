package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.SummarizeForm;
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

import java.io.OutputStream;
import java.util.List;
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


        return textPrompt.replace("${query}", query).replace("${supportingText}", supportingText);

    }
    private String getSystemPrompt(String query, String supportingText) {
        if (UtilMethods.isEmpty(query) || UtilMethods.isEmpty(supportingText)) {
            throw new DotRuntimeException("no query or supportingText to summarize found");

        }
        String  systemPrompt = config.get().getSearchSystemPrompt();


        return systemPrompt.replace("${query}", query).replace("${supportingText}", supportingText);

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





    private JSONObject buildRequestDataJson(SummarizeForm form){

        // get embeddings for query
        List<Float> queryEmbeddings = EmbeddingsAPI.impl().generateEmbeddingsforString(form.query);


        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                .withEmbeddings(queryEmbeddings)
                .withField(form.fieldVar)
                .withContentType(form.contentType)
                .withHost(form.site)
                .withQuery(form.query)
                .withIndexName(form.indexName)
                .withLimit(form.searchLimit)
                .withOperator(form.operator)
                .withThreshold(form.threshold)
                .withTokenCount(form.responseLengthTokens)
                .build();


        int maxTokenSize = maxTokensPerModel.getOrDefault(config.get().getModel(), 4096);



        // results from our database
        List<EmbeddingsDTO> searchResults = EmbeddingsDB.impl.get().searchEmbeddings(searcher);

        // aggregate matching results into text
        StringBuilder supportingText = new StringBuilder();
        searchResults.forEach(s -> supportingText.append(s.extractedText + " "));


        String systemPrompt = getSystemPrompt(searcher.query, supportingText.toString());
        String textPrompt = getTextPrompt(searcher.query, supportingText.toString());

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




    @Override
    public JSONObject summarize(SummarizeForm form) {



        // send all this as a json blob to OpenAI
        JSONObject json = buildRequestDataJson(form);


        String responseString = Try.of(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json.toString())).getOrElseThrow(e -> new DotRuntimeException(e));
        return new JSONObject(responseString);


    }
    @Override
    public void summarizeStream(SummarizeForm form, OutputStream out) {




        JSONObject json = buildRequestDataJson(form);
        json.put("stream", true);
        Try.run(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json.toString(), out)).getOrElseThrow(e -> new DotRuntimeException(e));


    }

}
