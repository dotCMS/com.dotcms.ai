package com.dotcms.embeddings.db;

import java.util.List;

public class ContentEmbeddings {
    final public Double[] embeddings;
    final public String identifier;
    final public String inode;
    final public long language;
    final public String title;
    final public String contentType;
    final public String field;
    final public String extractedText;


    public ContentEmbeddings(List<Double> embeddings, String identifier, String inode, long language, String title, String contentType, String field, String extractedText) {
        this.embeddings = embeddings.toArray(new Double[0]);
        this.identifier = identifier;
        this.inode = inode;
        this.language = language;
        this.title = title;
        this.contentType = contentType;
        this.field = field;
        this.extractedText = extractedText;
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
                ", extractedText='" + extractedText + '\'' +
                ", embeddings.size='" + embeddings.length + '\'' +
                '}';
    }

    public static final class Builder {
        private List<Double> embeddings;
        private String identifier;
        private String inode;
        private long language;
        private String title;
        private String contentType;
        private String field;
        private String extractedText;


        public static Builder aContentEmbeddings() {
            return new Builder();
        }

        public Builder withEmbeddings(List<Double> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withExtractedText(String extractedText) {
            this.extractedText = identifier;
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

        public ContentEmbeddings build() {
            return new ContentEmbeddings(embeddings, identifier, inode, language, title, contentType, field, extractedText);
        }
    }
}
