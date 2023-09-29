package com.dotcms.ai.db;

import java.util.List;

public class EmbeddingsDTO {
    public final Float[] embeddings;
    public final String identifier;
    public final String inode;
    public final long language;
    public final String title;
    public final String contentType;
    public final String field;
    public final String extractedText;
    public final String host;
    public final int limit;
    public final int offset;
    public final float threshold;

    public EmbeddingsDTO(List<Float> embeddings, String identifier, String inode, long language, String title, String contentType, String field, String extractedText, String host, int limit, int offset, float threshold) {
        this.embeddings = (embeddings == null) ? new Float[0] : embeddings.toArray(new Float[0]);
        this.identifier = identifier;
        this.inode = inode;
        this.language = language;
        this.title = title;
        this.contentType = contentType;
        this.field = field;
        this.extractedText = extractedText;
        this.host = host;
        this.limit = limit;
        this.offset = offset;
        this.threshold=threshold;
    }

    @Override
    public String toString() {
        return "ContentEmbeddings{" +
                "identifier='" + identifier + '\'' +
                ", inode='" + inode + '\'' +
                ", language=" + language +
                ", title='" + title + '\'' +
                ", contentType='" + contentType + '\'' +
                ", field='" + field + '\'' +
                ", host='" + host + '\'' +
                ", extractedText='" + extractedText + '\'' +
                ", embeddings.size='" + embeddings.length + '\'' +
                '}';
    }

    public static final class Builder {
        private List<Float> embeddings;
        private String identifier;
        private String inode;
        private long language;
        private String title;
        private String contentType;
        private String field;
        private String extractedText;
        private String host;
        private int limit = 100;
        private int offset = 0;
        public float threshold = .5f;

        public Builder withEmbeddings(List<Float> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }
        public Builder withThreshold(float threshold) {
            this.threshold = threshold;
            return this;
        }
        public Builder withExtractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }

        public Builder withInode(String inode) {
            this.inode = inode;
            return this;
        }

        public Builder withLanguage(long language) {
            this.language = language;
            return this;
        }

        public Builder withLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder withOffset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withField(String field) {
            this.field = field;
            return this;
        }

        public EmbeddingsDTO build() {
            return new EmbeddingsDTO(embeddings, identifier, inode, language, title, contentType, field, extractedText, host, limit, offset, threshold);
        }
    }
}
