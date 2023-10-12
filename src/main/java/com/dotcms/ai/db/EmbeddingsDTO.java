package com.dotcms.ai.db;

import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = EmbeddingsDTO.Builder.class)
public class EmbeddingsDTO implements Serializable {
    public final Float[] embeddings;
    public final String identifier;
    public final String inode;
    public final long language;
    public final String title;
    public final String contentType;
    public final String field;
    public final String extractedText;
    public final String host;
    public final String indexName;
    public final String operator;
    public final int limit;
    public final int tokenCount;
    public final int offset;
    public final float threshold;
    public final String query;
    public final String[] showFields;
    public final User user;

    private final String[] operators = {"<->", "<=>", "<#>"};

    private EmbeddingsDTO(Builder builder) {
        this.embeddings = (builder.embeddings == null) ? new Float[0] : builder.embeddings.toArray(new Float[0]);
        this.identifier = builder.identifier;
        this.inode = builder.inode;
        this.language = builder.language;
        this.title = builder.title;
        this.contentType = builder.contentType;
        this.field = builder.field;
        this.extractedText = builder.extractedText;
        this.host = builder.host;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.threshold = builder.threshold;
        this.operator = (Arrays.asList(operators).contains(builder.operator)) ? builder.operator : "<=>";
        this.indexName = UtilMethods.isSet(builder.indexName) ? builder.indexName : "default";
        this.tokenCount = builder.tokenCount;
        this.query = builder.query;
        this.user = builder.user;
        this.showFields = builder.showFields == null ? new String[0] : builder.showFields;
    }

    public static Builder from(CompletionsForm form) {
        return new Builder()
                .withField(form.fieldVar)
                .withContentType(form.contentType)
                .withHost(form.site)
                .withQuery(form.query)
                .withShowFields(form.fields)
                .withIndexName(form.indexName)
                .withLimit(form.searchLimit)
                .withOffset(form.searchOffset)
                .withOperator(form.operator)
                .withThreshold(form.threshold)
                .withTokenCount(form.responseLengthTokens);

    }

    public static Builder from(Map<String, Object> form) {

        return new Builder()
                .withField((String) form.get("fieldVar"))
                .withContentType((String) form.get("contentType"))
                .withLanguage(Try.of(() -> Long.parseLong((String) form.get("language"))).getOrElse(APILocator.getLanguageAPI().getDefaultLanguage().getId()))
                .withHost((String) form.get("host"))
                .withQuery((String) form.get("query"))
                .withIndexName((String) form.get("indexName"))
                .withLimit(Try.of(() -> Integer.parseInt((String) form.get("limit"))).getOrElse(100))
                .withOffset(Try.of(() -> Integer.parseInt((String) form.get("offset"))).getOrElse(0))
                .withOperator((String) form.get("operator"))
                .withThreshold((Try.of(() -> Float.parseFloat((String) form.get("threshold"))).getOrElse(.25f)));

    }

    public static Builder copy(EmbeddingsDTO values) {
        return new Builder()

                .withEmbeddings((values.embeddings == null) ? List.of() : Arrays.asList(values.embeddings))
                .withIdentifier(values.identifier)
                .withInode(values.inode)
                .withIndexName(values.indexName)
                .withLanguage(values.language)
                .withTitle(values.title)
                .withContentType(values.contentType)
                .withField(values.field)
                .withExtractedText(values.extractedText)
                .withHost(values.host)
                .withLimit(values.limit)
                .withOffset(values.offset)
                .withThreshold(values.threshold)
                .withOperator(values.operator)
                .withIndexName(values.indexName)
                .withTokenCount(values.tokenCount)
                .withShowFields(values.showFields)
                .withUser(values.user)
                .withQuery(values.query);

    }

    @Override
    public String toString() {
        return "EmbeddingsDTO{" +
                "identifier='" + identifier + '\'' +
                ", inode='" + inode + '\'' +
                ", language=" + language +
                ", title='" + title + '\'' +
                ", contentType='" + contentType + '\'' +
                ", field='" + field + '\'' +
                ", extractedText='" + extractedText + '\'' +
                ", host='" + host + '\'' +
                ", indexName='" + indexName + '\'' +
                ", operator='" + operator + '\'' +
                ", limit=" + limit +
                ", tokenCount=" + tokenCount +
                ", offset=" + offset +
                ", threshold=" + threshold +
                '}';
    }

    public static final class Builder implements Serializable {

        @JsonProperty(defaultValue = ".25f")
        public float threshold = .25f;
        @JsonProperty(defaultValue = "<=>")
        public String operator = "<=>";
        @JsonProperty
        int tokenCount = 0;
        @JsonProperty
        private List<Float> embeddings;
        @JsonProperty
        private String identifier;
        @JsonProperty
        private String inode;
        @JsonProperty
        private long language;
        @JsonProperty
        private String title;
        @JsonProperty
        private String contentType;
        @JsonProperty
        private String field;
        @JsonProperty(defaultValue = "default")
        private String indexName = "default";
        @JsonProperty
        private String extractedText;
        @JsonProperty
        private String host;
        @JsonProperty(defaultValue = "100")
        private int limit = 100;
        @JsonProperty(defaultValue = "0")
        private int offset = 0;
        @JsonProperty
        private String query;

        private User user;
        @JsonProperty(defaultValue = "")
        private String[] showFields;


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

        public Builder withQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder withIndexName(String indexName) {
            this.indexName = UtilMethods.isSet(indexName) ? indexName : "default";
            return this;
        }

        public Builder withOperator(String distanceOperator) {
            this.operator = distanceOperator;
            return this;
        }

        public Builder withShowFields(String[] showFields) {
            this.showFields = showFields;
            return this;
        }

        public Builder withUser(User user) {
            this.user = user;
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

        public Builder withTokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
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
            return new EmbeddingsDTO(this);
        }
    }
}
