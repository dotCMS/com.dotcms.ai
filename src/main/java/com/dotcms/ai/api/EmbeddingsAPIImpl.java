package com.dotcms.ai.api;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.util.ConfigProperties;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.ai.workflow.DotEmbeddingsActionlet;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EmbeddingsAPIImpl implements EmbeddingsAPI {


    final DotConcurrentFactory.SubmitterConfig submitterConfig = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS", 1))
            .maxPoolSize(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS_MAX", 4))
            .queueCapacity(ConfigProperties.getIntProperty("EMBEDDINGS_THREADS_QUEUE", 1000))
            .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
            .build();


    final Lazy<DotSubmitter> dotSubmitter = Lazy.of(() -> DotConcurrentFactory
            .getInstance().getSubmitter("embeddingsSubmitter", submitterConfig));


    @Override
    public void shutdown() {
        this.dotSubmitter.get().shutdown();
    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet) {
        generateEmbeddingsforContent(contentlet, Optional.empty());


    }

    @Override
    public void generateEmbeddingsforContent(Contentlet contentlet, Optional<Field> field) {

        dotSubmitter.get().submit(() -> {
            try {

                final String content = field.isPresent()
                        ? ContentToStringUtil.instance.get().parseField(field.get(), contentlet.getStringProperty(field.get().variable()))
                        : ContentToStringUtil.instance.get().guessWhatToIndex(contentlet);
                final String fieldVar = field.isPresent()
                        ? field.get().variable()
                        : contentlet.getContentType().variable();

                Logger.info(DotEmbeddingsActionlet.class, "found content for " + fieldVar + " : " + content);
                List<Double> embeddings = generateEmbeddingsforString(content);

                EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                        .withContentType(contentlet.getContentType().variable())
                        .withField(fieldVar)
                        .withInode(contentlet.getInode())
                        .withLanguage(contentlet.getLanguageId())
                        .withTitle(contentlet.getTitle())
                        .withIdentifier(contentlet.getIdentifier())
                        .withExtractedText(content.substring(0, Math.min(content.length(), 10000)))
                        .withEmbeddings(embeddings)
                        .build();


                EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);
            } catch (Throwable e) {
                Logger.error(EmbeddingsAPIImpl.class, e.getMessage(), e);

            }

        });
    }


    public List<Contentlet> search(@NotNull String prompt, String contentType, String fieldVar) {
        if (UtilMethods.isEmpty(prompt)) return List.of();
        prompt = UtilMethods.truncatify(prompt, 1024);
        // load embeddings for query
        List<Double> embeddings = generateEmbeddingsforString(prompt);


        return List.of();


    }

    @Override
    public List<Double> generateEmbeddingsforString(String stringToEncode) {
        List<Integer> encodedList = EncodingUtil.encoding.get().encode(stringToEncode);
        List<Double> embeddings = new ArrayList<>();
        batches(encodedList, 8000).forEach(l -> embeddings.addAll(generateEmbeddingsforTokens(l)));
        return embeddings;
    }


    List<Double> generateEmbeddingsforTokens(List<Integer> tokens) {

        JSONObject json = new JSONObject();
        json.put("model", ConfigProperties.getProperty("OPEN_AI_MODEL", "text-embedding-ada-002"));
        json.put("input", tokens);


        String responseString = Try.of(() -> OpenAIRequest.doRequest("https://api.openai.com/v1/embeddings", "post", json.toString())).getOrElseThrow(DotRuntimeException::new);
        JSONObject response = new JSONObject(responseString);

        JSONObject data = (JSONObject) response.getJSONArray("data").get(0);


        return (List<Double>) data.getJSONArray("embedding").stream().map(val -> {
            double x = (double) val;
            return (double) x;
        }).collect(Collectors.toList());
    }


    <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0) throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0) return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }


    List<Integer> getTokens(String fieldText) {
        return EncodingUtil.encoding.get().encode(fieldText);
    }


}
