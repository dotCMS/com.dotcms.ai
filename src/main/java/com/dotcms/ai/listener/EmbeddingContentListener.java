package com.dotcms.embeddings.listener;

import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.util.Logger;

public class EmbeddingContentListener implements ContentletListener<Contentlet> {


    @Override
    public String getId() {
        return EmbeddingContentListener.class.getCanonicalName();
    }


    @Subscriber
    @Override
    public void onModified(final ContentletCheckinEvent<Contentlet> contentletSaveEvent) {

        Contentlet contentlet = contentletSaveEvent.getContentlet();
        Logger.info(EmbeddingContentListener.class.getCanonicalName(), "Got save for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());


    }

    @Subscriber
    public void onPublish(final ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        Contentlet contentlet = contentletPublishEvent.getContentlet();
        Logger.info(EmbeddingContentListener.class.getCanonicalName(), "Got publish:" + contentletPublishEvent.isPublish() + " for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());
    }




    @Subscriber
    @Override
    public void onArchive(ContentletArchiveEvent<Contentlet> contentletArchiveEvent) {
        Contentlet contentlet = contentletArchiveEvent.getContentlet();
        Logger.info(EmbeddingContentListener.class.getCanonicalName(), "Got archive:" + contentletArchiveEvent.isArchive() + " for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());
    }

    @Subscriber
    @Override
    public void onDeleted(ContentletDeletedEvent<Contentlet> contentletDeletedEvent) {

        Contentlet contentlet = contentletDeletedEvent.getContentlet();
        Logger.info(EmbeddingContentListener.class.getCanonicalName(), "Got delete for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());


    }


}
