package com.dotcms.ai.rest.forms;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.web.WebAPILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(builder = SummarizeForm.Builder.class)
public class SummarizeForm extends Validated {

    @Size(min=1, max = 4096)
    public final String query;

    @Min(1)
    @Max(1000)
    public final int searchLimit;

    @Min(1)
    @Max(1000)
    public final int searchOffset;

    @Min(128)
    @Max(10240)
    public final int responseLengthTokens;

    public final boolean stream;
    public final String fieldVar;
    public final String indexName;
    public final String contentType;
    public final float threshold;
    public final String operator;
    public final String site;

    static final Map<String,String> OPERATORS=Map.of("distance", "<->", "cosine", "<=>", "innerProduct", "<#>");


    private SummarizeForm(SummarizeForm.Builder builder) {
        this.query = builder.query;
        this.searchLimit = builder.searchLimit;
        this.fieldVar = builder.fieldVar;
        this.responseLengthTokens = builder.responseLengthTokens;
        this.stream = builder.stream;
        this.indexName = builder.indexName;
        this.contentType = builder.contentType;
        this.threshold = builder.threshold;
        this.operator = OPERATORS.getOrDefault(builder.operator, "<=>");
        this.site = builder.site;
        this.searchOffset=builder.searchOffset;
    }


    public static final class Builder {
        @JsonSetter(nulls = Nulls.SKIP)
        private String query;

        @JsonSetter(nulls = Nulls.SKIP)
        private int searchLimit = 1000;

        @JsonSetter(nulls = Nulls.SKIP)
        private int searchOffset = 0;

        @JsonSetter(nulls = Nulls.SKIP)
        private int responseLengthTokens = 1024;

        @JsonSetter(nulls = Nulls.SKIP)
        private boolean stream = true;

        @JsonProperty
        private String fieldVar;

        @JsonProperty
        private String indexName;

        @JsonProperty
        private String contentType;

        @JsonSetter(nulls = Nulls.SKIP)
        private float threshold = .8f;

        @JsonSetter(nulls = Nulls.SKIP)
        private String operator = "cosine";

        @JsonSetter(nulls = Nulls.SKIP)
        private String site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()).getIdentifier();





        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder searchLimit(int searchLimit) {
            this.searchLimit = searchLimit;
            return this;
        }
        public Builder searchOffset(int searchOffset) {
            this.searchOffset = searchOffset;
            return this;
        }
        public Builder responseLengthTokens(int responseLengthTokens) {
            this.responseLengthTokens = responseLengthTokens;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder fieldVar(String fieldVar) {
            this.fieldVar = fieldVar;
            return this;
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public SummarizeForm build() {
            return new SummarizeForm(this);

        }
    }
}
