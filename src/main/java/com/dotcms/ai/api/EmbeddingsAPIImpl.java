package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.ContentResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;



public class EmbeddingsAPIImpl implements EmbeddingsAPI {


    final AppConfig config;
    final Cache<String,Tuple2<Integer, List<Float>>> embeddingCache ;
    static Lazy<DotSubmitter> dotSubmitter ;


    EmbeddingsAPIImpl(Host host) {

        this.config = ConfigService.INSTANCE.config(host);
        embeddingCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(this.config.getConfig(AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS, 1000)))
                .maximumSize(this.config.getConfig(AppKeys.EMBEDDINGS_CACHE_SIZE, 1000))
                .build();

        dotSubmitter =getDotSubmitter();
    }

    private EmbeddingsAPIImpl(){
        throw new DotRuntimeException("unable to initialize EmbeddingsAPIImpl");
    }


    private Lazy<DotSubmitter> getDotSubmitter(){
        return Lazy.of(() -> DotConcurrentFactory.getInstance().getSubmitter("embeddingsSubmitter", new DotConcurrentFactory.SubmitterConfigBuilder()
                .poolSize(this.config.getConfig(AppKeys.EMBEDDINGS_THREADS, 1))
                .maxPoolSize(this.config.getConfig(AppKeys.EMBEDDINGS_THREADS_MAX, 10))
                .queueCapacity(this.config.getConfig(AppKeys.EMBEDDINGS_THREADS_QUEUE, 10000))
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build()));
    }


    @Override
    public void shutdown() {
        Try.run(()->dotSubmitter.get().shutdown());
    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet, String indexName) {
        generateEmbeddingsforContent(contentlet, Optional.empty(), indexName);
    }

    @Override
    public int deleteEmbedding(@NotNull EmbeddingsDTO dto) {


        return EmbeddingsDB.impl.get().deleteEmbeddings(dto);


    }

    final static String MATCHES = "dotMatches";
    @Override
    public JSONObject searchEmbedding(EmbeddingsDTO searcher){

        long startTime = System.currentTimeMillis();

        List<EmbeddingsDTO> searchResults = getEmbeddingResults(searcher);
        Map<String, Object> reducedResults = new LinkedHashMap<>();
        Set<String> fields = Arrays.stream(searcher.showFields).collect(Collectors.toSet());


        for (EmbeddingsDTO result : searchResults) {

            JSONObject contentObject = (JSONObject) Try.of(()->
                            ContentResource.contentletToJSON(
                                    APILocator.getContentletAPI().find(result.inode, searcher.user, true),
                                    HttpServletRequestThreadLocal.INSTANCE.getRequest(),
                                    HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                                    "false",
                                    searcher.user,
                                    false)
                    ).andThenTry(()->
                    new JSONObject(APILocator.getContentletAPI().find(result.inode, searcher.user, true).getMap())
            ).getOrNull();
            if(contentObject==null){
                continue;
            }

            if (fields.size() > 1) {
                contentObject.keySet().removeIf(k -> !fields.contains(k));
            }


            JSONArray matches = contentObject.optJSONArray(MATCHES) == null ? new JSONArray() : contentObject.optJSONArray(MATCHES);
            JSONObject match = new JSONObject();
            match.put("distance", result.threshold);
            match.put("extractedText", result.extractedText);
            matches.add(match);
            contentObject.put(MATCHES, matches);

            if (!reducedResults.containsKey(result.inode)) {
                reducedResults.put(result.inode, contentObject);
            }

        }


        long totalTime = System.currentTimeMillis() - startTime;
        Map<String, Long> countMatches = EmbeddingsAPI.impl().countEmbeddings(searcher);

        JSONObject map = new JSONObject();
        map.put("timeToEmbeddings", totalTime + "ms");
        map.put("total", searchResults.size());
        map.put("threshold", searcher.threshold);
        map.put("results", reducedResults.values());
        map.put("operator", searcher.operator);
        map.put("count", countMatches);

        return map;


    }


    @Override
    public void generateEmbeddingsforContent(@NotNull Contentlet contentlet, Optional<Field> tryField, String indexName) {


        final Optional<Field> field = tryField.isPresent() ? tryField : ContentToStringUtil.impl.get().guessWhatFieldToIndex(contentlet);
        final Optional<String> content = ContentToStringUtil.impl.get().parseField(contentlet, field);


        this.dotSubmitter.get().submit(() -> {

            try {

                if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
                    Logger.info(EmbeddingsAPIImpl.class, "Skipping/No Content for contentlet:" + contentlet.getContentType().variable() + " id:" + contentlet.getIdentifier() + " title:" + contentlet.getTitle());
                    return;
                }

                final String cleanContent = String.join(" ", content.get().trim().split("\\s+"));
                final int SPLIT_AT_WORDS = config.getConfig(AppKeys.EMBEDDINGS_SPLIT_AT_WORDS, 65);

                // split into sentences
                BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
                StringBuilder buffer = new StringBuilder();
                iterator.setText(cleanContent);
                List<String> paragraphs = new ArrayList<>();
                int start = iterator.first();
                for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                    buffer.append(cleanContent.substring(start, end) + " ");
                    if (buffer.toString().split("\\s+").length > SPLIT_AT_WORDS) {
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

    @Override
    public List<Float> generateEmbeddingsforString(String stringToEncode) {
        if(UtilMethods.isEmpty(stringToEncode)){
            return List.of();
        }
        List<Integer> encodedList = EncodingUtil.encoding.get().encode(stringToEncode);
        List<Float> embeddings = new ArrayList<>();
        batches(encodedList, 8000).forEach(l -> embeddings.addAll(generateEmbeddingsforTokens(l)));
        return embeddings;
    }

    @Override
    public List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = generateEmbeddingsforString(searcher.query);


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().searchEmbeddings(newSearcher);

    }

    @Override
    public Map<String,Long> countEmbeddings(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = generateEmbeddingsforString(searcher.query);


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().countEmbeddings(newSearcher);

    }

    @Override
    public Map<String,Long> countEmbeddingsByIndex() {
        return EmbeddingsDB.impl.get().countEmbeddingsByIndex();

    }



    @Override
    public void dropEmbeddingsTable() {


        EmbeddingsDB.impl.get().dropVectorDbTable();


    }

    @Override
    public void initEmbeddingsTable() {

        EmbeddingsDB.impl.get().initVectorExtension();
        EmbeddingsDB.impl.get().initVectorDbTable();


    }

    private <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0) throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0) return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

    private void saveEmbedding(@NotNull String content, @NotNull Contentlet contentlet, Optional<Field> field, String indexName) {
        final String fieldVar = field.isPresent() ? field.get().variable() : contentlet.getContentType().variable();
        if (UtilMethods.isEmpty(content)) {
            return;
        }
        Tuple2<Integer, List<Float>> embeddings = pullOrGenerateEmbeddings(content);

        if (embeddings._2.isEmpty()) {
            Logger.info(this.getClass(), "NO TOKENS for " + contentlet.getContentType().variable() + "." + fieldVar + " : " + content);
            return;
        }


        EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(contentlet.getContentType().variable())
                .withField(fieldVar)
                .withTokenCount(embeddings._1)
                .withInode(contentlet.getInode())
                .withLanguage(contentlet.getLanguageId())
                .withTitle(contentlet.getTitle())
                .withIdentifier(contentlet.getIdentifier())
                .withHost(contentlet.getHost())
                .withExtractedText(content)
                .withIndexName(indexName)
                .withEmbeddings(embeddings._2).build();


        EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);


    }
    private String hashText(String text) {

        return EmbeddingsDB.impl.get().hashText(text);
    }

    /**
     * this method takes a snippet of content and will try to see if we have already generated embeddings for it.
     * It checks the cache first, and returns if it finds it there.  Then it checks the db to see if we have already
     * saved this chunk of content before.  If we have, we reuse those same embeddings rather than making a
     * remote request $$$ to OpenAI for new Embeddings
     *
     * @param content
     * @return
     */
    private Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String content) {
        String hashed = hashText(content);
        Tuple2<Integer, List<Float>> alreadyHaveEmbeddings = embeddingCache.getIfPresent(hashed);
        if (alreadyHaveEmbeddings == null || alreadyHaveEmbeddings._2.isEmpty()) {

            alreadyHaveEmbeddings = EmbeddingsDB.impl.get().searchExistingEmbeddings(content);
            if (alreadyHaveEmbeddings == null || alreadyHaveEmbeddings._2.isEmpty()) {
                List<Integer> tokens = EncodingUtil.encoding.get().encode(content);
                if (tokens.isEmpty()) {
                    Logger.debug(this.getClass(), "NO TOKENS for " + content);
                    return Tuple.of(0, List.of());
                }
                alreadyHaveEmbeddings = Tuple.of(tokens.size(), generateEmbeddingsforTokens(tokens));
            }
            embeddingCache.put(hashed, alreadyHaveEmbeddings);
        }
        return alreadyHaveEmbeddings;
    }




    private List<Float> generateEmbeddingsforTokens(@NotNull List<Integer> tokens) {

        JSONObject json = new JSONObject();

        json.put("model", config.getConfig(AppKeys.EMBEDDINGS_MODEL,"text-embedding-ada-002"));
        json.put("input", tokens);


        String responseString = Try.of(() -> OpenAIRequest.doRequest("https://api.openai.com/v1/embeddings", "post", getAPIKey(), json.toString())).getOrElseThrow(() -> new DotRuntimeException("No API Key Available"));
        JSONObject response = new JSONObject(responseString);

        JSONObject data = (JSONObject) response.getJSONArray("data").get(0);


        return (List<Float>) data.getJSONArray("embedding").stream().map(val -> {
            double x = (double) val;
            return (float) x;
        }).collect(Collectors.toList());
    }

    private String getAPIKey() {
        return config.getApiKey();

    }

    private List<Integer> getTokens(String fieldText) {
        return EncodingUtil.encoding.get().encode(fieldText);
    }


}
