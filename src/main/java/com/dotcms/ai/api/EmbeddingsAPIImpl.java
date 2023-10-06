package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.ConfigProperties;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.ai.workflow.DotEmbeddingsActionlet;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import joptsimple.internal.Strings;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EmbeddingsAPIImpl implements EmbeddingsAPI {


    final AppConfig config;
    final DotConcurrentFactory.SubmitterConfig submitterConfig = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS", 1))
            .maxPoolSize(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS_MAX", 3))
            .queueCapacity(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS_QUEUE", 1000))
            .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
            .build();
    final Lazy<DotSubmitter> dotSubmitter = Lazy.of(() -> DotConcurrentFactory.getInstance().getSubmitter("embeddingsSubmitter", submitterConfig));


    EmbeddingsAPIImpl(AppConfig config) {

        this.config = (config != null) ? config : Try.of(() -> ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest())).get()).getOrNull();
    }

    @Override
    public void shutdown() {
        this.dotSubmitter.get().shutdown();
    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet, String indexName) {
        generateEmbeddingsforContent(contentlet, Optional.empty(),indexName);
    }

    @Override
    public int deleteEmbedding(EmbeddingsDTO dto) {


        return EmbeddingsDB.impl.get().deleteEmbeddings(dto);


    }

    @Override
    public List<EmbeddingsDTO> searchEmbedding(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = generateEmbeddingsforString(searcher.query);


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().searchEmbeddings(newSearcher);

    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet, Optional<Field> field, String indexName) {

        dotSubmitter.get().submit(() -> {
            try {

                final Tuple2<String, String> fieldThenContent = field.isPresent() ? Tuple.of(field.get().variable(), ContentToStringUtil.impl.get().parseField(field.get(), contentlet.getStringProperty(field.get().variable()))) : ContentToStringUtil.impl.get().guessWhatToIndex(contentlet);

                if (UtilMethods.isEmpty(() -> fieldThenContent._2)) {
                    Logger.info(EmbeddingsAPIImpl.class, "No content found for contentlet id:" + contentlet.getIdentifier() + " title:" + contentlet.getTitle());
                }
                final String cleanContent = Strings.join(fieldThenContent._2.trim().split("\\s+"), " ");

                // split into sentences
                BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
                StringBuilder buffer = new StringBuilder();
                iterator.setText(cleanContent);
                List<String> paragraphs = new ArrayList<>();
                int start = iterator.first();
                for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                    buffer.append(cleanContent.substring(start, end) + " ");
                    if (buffer.toString().split("\\s+").length > 65) {
                        paragraphs.add(buffer.toString());
                        buffer.setLength(0);
                    }
                }
                if (buffer.toString().split("\\s+").length > 0) {
                    paragraphs.add(buffer.toString());
                }

                for (String paragraph : paragraphs) {
                    saveEmbedding(paragraph, contentlet, field, indexName);
                }


            } catch (Throwable e) {
                Logger.error(EmbeddingsAPIImpl.class, e.getMessage(), e);

            }

        });
    }

    private void saveEmbedding(String content,Contentlet contentlet, Optional<Field> field,String indexName ){
        final String fieldVar = field.isPresent() ? field.get().variable() : contentlet.getContentType().variable();
        if(UtilMethods.isEmpty(content)){
            return;
        }

        List<Integer> tokens = EncodingUtil.encoding.get().encode(content);
        if(tokens.isEmpty()){
            Logger.debug(DotEmbeddingsActionlet.class, "NO TOKENS for " + contentlet.getContentType().variable() + "." + fieldVar + " : " + content);
            return;
        }
        Logger.info(DotEmbeddingsActionlet.class, "found content for " + contentlet.getContentType().variable() + "." + fieldVar + " : " + content);
        List<Float> embeddings = generateEmbeddingsforTokens(tokens);

        EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(contentlet.getContentType().variable())
                .withField(fieldVar)
                .withTokenCount(tokens.size())
                .withInode(contentlet.getInode())
                .withLanguage(contentlet.getLanguageId())
                .withTitle(contentlet.getTitle())
                .withIdentifier(contentlet.getIdentifier())
                .withHost(contentlet.getHost())
                .withExtractedText(content)
                .withIndexName(indexName)
                .withEmbeddings(embeddings).build();


        EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);




    }


    private static String API_KEY=null;
    private static long lastAPIKeyUpdate=0;
    String getAPIKey(){
        if(API_KEY == null || System.currentTimeMillis() > lastAPIKeyUpdate+60000){
            synchronized (this.getClass()) {
                if(API_KEY == null || System.currentTimeMillis() > lastAPIKeyUpdate+60000) {
                    API_KEY = config.getApiKey();
                    lastAPIKeyUpdate = System.currentTimeMillis();
                }
            }
        }
        return API_KEY;

    }



    @Override
    public List<Float> generateEmbeddingsforString(String stringToEncode) {
        List<Integer> encodedList = EncodingUtil.encoding.get().encode(stringToEncode);
        List<Float> embeddings = new ArrayList<>();
        batches(encodedList, 8000).forEach(l -> embeddings.addAll(generateEmbeddingsforTokens(l)));
        return embeddings;
    }

    @Override
    public void dropEmbeddingsTable() {


        EmbeddingsDB.impl.get().dropVectorDbTable();


    }

    <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0) throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0) return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

    List<Float> generateEmbeddingsforTokens(List<Integer> tokens) {

        JSONObject json = new JSONObject();
        json.put("model", ConfigProperties.getProperty("OPEN_AI_MODEL", "text-embedding-ada-002"));
        json.put("input", tokens);


        String responseString = Try.of(() -> OpenAIRequest.doRequest("https://api.openai.com/v1/embeddings", "post", getAPIKey(), json.toString())).getOrElseThrow(() -> new DotRuntimeException("No API Key Available"));
        JSONObject response = new JSONObject(responseString);

        JSONObject data = (JSONObject) response.getJSONArray("data").get(0);


        return (List<Float>) data.getJSONArray("embedding").stream().map(val -> {
            double x = (double) val;
            return (float) x;
        }).collect(Collectors.toList());
    }

    @Override
    public void initEmbeddingsTable() {

        EmbeddingsDB.impl.get().initVectorExtension();
        EmbeddingsDB.impl.get().initVectorDbTable();


    }

    public List<Contentlet> search(@NotNull String prompt, String contentType, String fieldVar) {
        if (UtilMethods.isEmpty(prompt)) return List.of();
        prompt = UtilMethods.truncatify(prompt, 1024);
        // load embeddings for query
        List<Float> embeddings = generateEmbeddingsforString(prompt);


        return List.of();


    }

    List<Integer> getTokens(String fieldText) {
        return EncodingUtil.encoding.get().encode(fieldText);
    }


}
