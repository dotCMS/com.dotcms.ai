package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.ContentResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class EmbeddingsAPIImpl implements EmbeddingsAPI {


    static final Cache<String, Tuple2<Integer, List<Float>>> embeddingCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS)))
            .maximumSize(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_CACHE_SIZE))
            .build();


    static final String MATCHES = "matches";
    final AppConfig config;

    EmbeddingsAPIImpl(Host host) {
        this.config = ConfigService.INSTANCE.config(host);
    }




    @Override
    public void shutdown() {
        Try.run(() -> OpenAIThreadPool.threadPool.get().shutdown());
    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet, String indexName) {
        generateEmbeddingsforContent(contentlet, Optional.empty(), indexName);
    }

    @Override
    public int deleteEmbedding(@NotNull EmbeddingsDTO dto) {


        return EmbeddingsDB.impl.get().deleteEmbeddings(dto);


    }

    @Override
    public void generateEmbeddingsforContent(@NotNull Contentlet contentlet, Optional<Field> tryField, String indexName) {


        final Optional<Field> field = tryField.isPresent() ? tryField : ContentToStringUtil.impl.get().guessWhatFieldToIndex(contentlet);
        final Optional<String> content = ContentToStringUtil.impl.get().parseField(contentlet, field);

        if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
            String fieldVar = Try.of(() -> field.get().variable()).getOrElse("unknown");
            Logger.info(EmbeddingsAPIImpl.class, "No content to embed for:" + contentlet.getContentType().variable() + "." + fieldVar + " id:" + contentlet.getIdentifier() + " title:" + contentlet.getTitle());
            return;
        }

        OpenAIThreadPool.threadPool.get().submit(new EmbeddingsRunner(field, contentlet, content.get(), indexName));

    }

    @Override
    public JSONObject reduceChunksToContent(EmbeddingsDTO searcher, final List<EmbeddingsDTO> searchResults) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> reducedResults = new LinkedHashMap<>();
        Set<String> fields = (searcher.showFields != null)
                ? Arrays.stream(searcher.showFields).collect(Collectors.toSet())
                : Set.of();

        for (EmbeddingsDTO result : searchResults) {

            JSONObject contentObject = dtoToContentJson(result, searcher.user);
            contentObject.getAsMap().computeIfAbsent("title", k -> result.title);

            if (contentObject == null) {
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


        long count = EmbeddingsAPI.impl().countEmbeddings(searcher);

        JSONObject map = new JSONObject();
        map.put("timeToEmbeddings", System.currentTimeMillis() - startTime + "ms");
        map.put("total", searchResults.size());
        map.put("query", searcher.query);
        map.put("threshold", searcher.threshold);
        map.put("dotCMSResults", reducedResults.values());
        map.put("operator", searcher.operator);
        map.put("offset", searcher.offset);
        map.put("limit", searcher.limit);
        map.put("count", count);

        return map;


    }

    @Override
    public JSONObject searchForContent(EmbeddingsDTO searcher) {

        long startTime = System.currentTimeMillis();

        List<EmbeddingsDTO> searchResults = getEmbeddingResults(searcher);
        JSONObject reducedResults = reduceChunksToContent(searcher, searchResults);

        long totalTime = System.currentTimeMillis() - startTime;

        reducedResults.put("timeToEmbeddings", totalTime + "ms");
        return reducedResults;


    }

    private JSONObject dtoToContentJson(EmbeddingsDTO dto, User user) {
        return Try.of(() ->
                ContentResource.contentletToJSON(
                        APILocator.getContentletAPI().find(dto.inode, user, true),
                        HttpServletRequestThreadLocal.INSTANCE.getRequest(),
                        HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                        "false",
                        user,
                        false)
        ).andThenTry(() ->
                new JSONObject(APILocator.getContentletAPI().find(dto.inode, user, true).getMap())
        ).andThenTry(() ->
                new JSONObject(Map.of("inode", dto.inode,
                        "identifier", dto.identifier,
                        "title", dto.title,
                        "language", dto.language,
                        "field", dto.field,
                        "index", dto.indexName,
                        "contentType", dto.contentType))

        ).getOrElse(JSONObject::new);


    }

    private String hashText(String text) {

        return EmbeddingsDB.impl.get().hashText(text);
    }

    private List<Float> sendTokensToOpenAI(@NotNull List<Integer> tokens) {

        JSONObject json = new JSONObject();

        json.put("model", config.getConfig(AppKeys.EMBEDDINGS_MODEL));
        json.put("input", tokens);


        String responseString = OpenAIRequest.doRequest("https://api.openai.com/v1/embeddings", "post", getAPIKey(), json);
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

    @Override
    public List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = pullOrGenerateEmbeddings(searcher.query)._2;


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().searchEmbeddings(newSearcher);

    }

    @Override
    public long countEmbeddings(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = pullOrGenerateEmbeddings(searcher.query)._2;


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().countEmbeddings(newSearcher);

    }

    @Override
    public Map<String, Map<String, Long>> countEmbeddingsByIndex() {
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

    /**
     * this method takes a snippet of content and will try to see if we have already generated embeddings for it.
     * It checks the cache first, and returns if it finds it there.  Then it checks the db to see if we have already
     * saved this chunk of content before.  If we have, we reuse those same embeddings rather than making a
     * remote request $$$ to OpenAI for new Embeddings
     *
     * @param content
     * @return Tuple(Count of Tokens Input, List of Embeddings Output)
     */
    @Override
    public Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String content) {
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
                alreadyHaveEmbeddings = Tuple.of(tokens.size(), sendTokensToOpenAI(tokens));
            }
            embeddingCache.put(hashed, alreadyHaveEmbeddings);
        }
        return alreadyHaveEmbeddings;
    }



    class EmbeddingsRunner implements Runnable {
        final Optional<Field> field;
        final Contentlet contentlet;
        final String content;
        final String indexName;

        public EmbeddingsRunner(Optional<Field> field, Contentlet contentlet, String content, String index) {
            this.field = field;
            this.contentlet = contentlet;
            this.content = content;
            this.indexName = index;
        }

        @Override
        public void run() {
            try {

                final String fieldVar = field.isPresent() ? field.get().variable() : contentlet.getContentType().variable();


                if (config.getConfigBoolean(AppKeys.EMBEDDINGS_DB_DELETE_OLD_ON_UPDATE)) {

                    EmbeddingsDTO deleteOldVersions = new EmbeddingsDTO.Builder()
                            .withIdentifier(contentlet.getIdentifier())
                            .withLanguage(contentlet.getLanguageId())
                            .withIndexName(indexName)
                            .withField(fieldVar)
                            .withContentType(contentlet.getContentType().variable())
                            .build();
                    EmbeddingsDB.impl.get().deleteEmbeddings(deleteOldVersions);
                }


                final String cleanContent = String.join(" ", content.trim().split("\\s+"));
                final int SPLIT_AT_TOKENS = config.getConfigInteger(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS);

                // split into sentences
                BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
                StringBuilder buffer = new StringBuilder();
                iterator.setText(cleanContent);
                int start = iterator.first();
                int totalTokens = 0;
                for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                    String sentence = cleanContent.substring(start, end);
                    int tokenCount = EncodingUtil.encoding.get().countTokens(sentence);
                    totalTokens += tokenCount;

                    if (totalTokens < SPLIT_AT_TOKENS) {
                        buffer.append(sentence).append(" ");
                    } else {
                        saveEmbedding(buffer.toString(), contentlet, field, indexName);
                        buffer.setLength(0);
                        buffer.append(sentence).append(" ");
                        totalTokens = tokenCount;
                    }

                }
                if (buffer.toString().split("\\s+").length > 0) {
                    saveEmbedding(buffer.toString(), contentlet, field, indexName);
                }


            } catch (Exception e) {
                Logger.error(EmbeddingsAPIImpl.class, e.getMessage(), e);


            }
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

    }


}
