package com.dotcms.ai.listener;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.Logger;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import java.util.Map.Entry;
import java.util.Set;


public class EmbeddingContentListener implements ContentletListener<Contentlet> {


    @Override
    public String getId() {
        return EmbeddingContentListener.class.getCanonicalName();
    }




    @Override
    public void onModified(ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        Contentlet contentlet = contentletPublishEvent.getContentlet();

        if (contentletPublishEvent.isPublish()) {
            logEvent("onModified - PublishEvent:true ", contentlet);


        } else {
            logEvent("onModified - PublishEvent:false ", contentlet);
            deleteFromIndexes(contentlet);
        }
    }

    @Subscriber
    public void onPublish(final ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        Contentlet contentlet = contentletPublishEvent.getContentlet();
        if (contentletPublishEvent.isPublish()) {
            logEvent("onPublish - PublishEvent:true", contentlet);

            addToIndexesIfNeeded(contentlet);
        } else {
            logEvent("onPublish - PublishEvent:false", contentlet);
            deleteFromIndexes(contentlet);
        }
    }


    @Subscriber
    @Override
    public void onArchive(ContentletArchiveEvent<Contentlet> contentletArchiveEvent) {
        Contentlet contentlet = contentletArchiveEvent.getContentlet();
        logEvent("onArchive", contentlet);
        deleteFromIndexes(contentlet);
    }

    @Subscriber
    @Override
    public void onDeleted(ContentletDeletedEvent<Contentlet> contentletDeletedEvent) {

        Contentlet contentlet = contentletDeletedEvent.getContentlet();
        logEvent("onDeleted", contentlet);
        deleteFromIndexes(contentlet);
    }

    /**
     * JSONObject that has a list of indexes and the content types that should be indexed in them.
     *
     * @param contentlet
     * @return
     */
    JSONObject getConfigJson(Contentlet contentlet) {
        Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false))
                .getOrElse(APILocator.systemHost());

        return Try.of(() -> new JSONObject(ConfigService.INSTANCE.config(host).getConfig(AppKeys.LISTENER_INDEXER)))
                .getOrElse(new JSONObject());
    }

    /**
     * Adds the content to the embeddings index based on the JSON configuration in the app
     *
     * @param contentlet
     */
    void addToIndexesIfNeeded(Contentlet contentlet) {

        String contentType = contentlet.getContentType().variable();
        if (contentType == null) {
            return;
        }
        JSONObject config = getConfigJson(contentlet);

        for (Entry<String, Object> entry : (Set<Entry<String, Object>>) config.entrySet()) {
            final String indexName = entry.getKey();
            EmbeddingsAPI.impl()
                    .parseTypesAndFields((String) entry.getValue()).entrySet()
                    .stream()
                    .filter(e -> contentType.equalsIgnoreCase(e.getKey()))
                    .forEach(e ->
                            EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, e.getValue(), indexName)
                    );
        }
    }

    void deleteFromIndexes(Contentlet contentlet) {
        EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIdentifier(contentlet.getIdentifier())
                .withLanguage(contentlet.getLanguageId())
                .build();
        EmbeddingsAPI.impl().deleteEmbedding(dto);

    }

    private void logEvent(String eventType, Contentlet contentlet) {
        Logger.info(EmbeddingContentListener.class.getCanonicalName(),
                "GOT " + eventType + " for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());
    }


}
