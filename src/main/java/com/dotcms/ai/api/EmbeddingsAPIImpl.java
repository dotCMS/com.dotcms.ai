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
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rest.ContentResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
        Try.run(() -> OpenAIThreadPool.threadPool().shutdown());
    }

    @Override
    public boolean generateEmbeddingsforContent(Contentlet contentlet, String indexName) {
        return generateEmbeddingsforContent(contentlet, List.of(), indexName);
    }

    @Override
    public int deleteEmbedding(@NotNull EmbeddingsDTO dto) {
        return EmbeddingsDB.impl.get().deleteEmbeddings(dto);
    }

    @Override
    public boolean generateEmbeddingsforContent(@NotNull Contentlet contentlet, List<Field> tryFields, String indexName) {


        final List<Field> fields = tryFields.isEmpty() ? ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet) : tryFields;

        final Optional<String> content = ContentToStringUtil.impl.get().parseFields(contentlet, fields);

        if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
            Logger.info(EmbeddingsAPIImpl.class, "No valid fields to embed for:" + contentlet.getContentType().variable() + " id:" + contentlet.getIdentifier() + " title:" + contentlet.getTitle());
            return false;
        }

        OpenAIThreadPool.threadPool().submit(new EmbeddingsRunner(this, contentlet, content.get(), indexName));
        return true;
    }

    @Override
    public boolean generateEmbeddingsforContent(@NotNull Contentlet contentlet, String velocityTemplate, String indexName) {
        if (UtilMethods.isEmpty(velocityTemplate)) {
            return false;
        }
        final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), true)).getOrElse(APILocator.systemHost());
        User user = PortalUtil.getUser() != null ? PortalUtil.getUser() : APILocator.systemUser();


        HttpServletRequest requestProxy = new FakeHttpRequest("localhost", null).request();
        HttpServletResponse responseProxy = new BaseResponse().response();

        Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        ContentMap contentMap = new ContentMap(contentlet, user, PageMode.EDIT_MODE, host, ctx);
        ctx.put("dotContentMap", contentMap);
        ctx.put("contentlet", contentMap);
        ctx.put("content", contentMap);

        String textToEmbed = Try.of(() -> VelocityUtil.eval(velocityTemplate, ctx)).getOrNull();

        if (UtilMethods.isEmpty(textToEmbed)) {
            return false;
        }
        Optional<String> parsed = ContentToStringUtil.impl.get().isHtml(textToEmbed) ? ContentToStringUtil.impl.get().parseHTML(textToEmbed) : Optional.ofNullable(textToEmbed);

        if(parsed.isEmpty()){
            return false;
        }


        OpenAIThreadPool.threadPool().submit(new EmbeddingsRunner(this, contentlet, parsed.get(), indexName));
        return true;

    }


    @Override
    public JSONObject reduceChunksToContent(EmbeddingsDTO searcher, final List<EmbeddingsDTO> searchResults) {
        long startTime = System.currentTimeMillis();

        Map<String, JSONObject> reducedResults = new LinkedHashMap<>();
        Set<String> fields = (searcher.showFields != null)
                ? Arrays.stream(searcher.showFields).collect(Collectors.toSet())
                : Set.of();

        for (EmbeddingsDTO result : searchResults) {
            JSONObject contentObject = reducedResults.getOrDefault(result.inode, dtoToContentJson(result, searcher.user));

            contentObject.getAsMap().computeIfAbsent("title", k -> result.title);

            if (contentObject == null) {
                continue;
            }

            if (fields.size() > 0) {
                contentObject.keySet().removeIf(k -> !fields.contains(k));
            }

            JSONArray matches = contentObject.optJSONArray(MATCHES) == null ? new JSONArray() : contentObject.optJSONArray(MATCHES);
            JSONObject match = new JSONObject();
            match.put("distance", result.threshold);
            match.put("extractedText", UtilMethods.truncatify(result.extractedText, 255));
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
                        "index", dto.indexName,
                        "contentType", new JSONArray(dto.contentType)))

        ).getOrElse(JSONObject::new);


    }

    private String hashText(@NotNull String text) {

        return EmbeddingsDB.impl.get().hashText(text);
    }

    private void saveEmbeddingsForCache(String content, Tuple2<Integer, List<Float>> embeddings) {

        EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType("cache")
                .withTokenCount(embeddings._1)
                .withInode("cache")
                .withLanguage(0)
                .withTitle("cache")
                .withIdentifier("cache")
                .withHost("cache")
                .withExtractedText(content)
                .withIndexName("cache")
                .withEmbeddings(embeddings._2).build();


        EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);


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
    public Map<String, Map<String, Object>> countEmbeddingsByIndex() {
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
    public Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(@NotNull String content) {
        if (UtilMethods.isEmpty(content)) {
            return Tuple.of(0, List.of());
        }
        String hashed = hashText(content);
        final Tuple2<Integer, List<Float>> cachedEmbeddings = embeddingCache.getIfPresent(hashed);
        if (cachedEmbeddings != null && !cachedEmbeddings._2.isEmpty()) {
            return cachedEmbeddings;
        }

        List<Integer> tokens = EncodingUtil.encoding.get().encode(content);
        if (tokens.isEmpty()) {
            Logger.debug(this.getClass(), "NO TOKENS for " + content);
            return Tuple.of(0, List.of());
        }

        Tuple3<String, Integer, List<Float>> dbEmbeddings = EmbeddingsDB.impl.get().searchExistingEmbeddings(content);
        if (dbEmbeddings != null && !dbEmbeddings._3.isEmpty()) {
            if (!"cache".equalsIgnoreCase(dbEmbeddings._1)) {
                saveEmbeddingsForCache(content, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            }
            embeddingCache.put(hashed, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            return Tuple.of(dbEmbeddings._2, dbEmbeddings._3);
        }


        Tuple2<Integer, List<Float>> openAiEmbeddings = Tuple.of(tokens.size(), sendTokensToOpenAI(tokens));
        saveEmbeddingsForCache(content, openAiEmbeddings);
        embeddingCache.put(hashed, openAiEmbeddings);
        return openAiEmbeddings;


    }


}
