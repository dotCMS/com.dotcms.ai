package com.dotcms.ai.rest.forms;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vavr.control.Try;


import java.util.Map;

@JsonDeserialize(builder = CompletionsForm.Builder.class)
public class CompletionsForm extends Validated {

    @Size(min=1, max = 4096)
    public final String query;

    @Min(1)
    @Max(1000)
    public final int searchLimit;

    @Min(0)
    public final int searchOffset;

    @Min(128)
    @Max(10240)
    public final int responseLengthTokens;

    public final long language;


    public final boolean stream;
    public final String fieldVar;
    public final String indexName;
    public final String contentType;
    public final float threshold;
    public final String operator;
    public final String site;
    public final String[] fields;
    static final Map<String,String> OPERATORS=Map.of("distance", "<->", "cosine", "<=>", "innerProduct", "<#>");


    private CompletionsForm(CompletionsForm.Builder builder) {
        this.query = validateBuilderQuery(builder.query);
        this.searchLimit = builder.searchLimit;
        this.fieldVar = builder.fieldVar;
        this.responseLengthTokens = builder.responseLengthTokens;
        this.stream = builder.stream;
        this.indexName = builder.indexName ;
        this.contentType = builder.contentType;
        this.threshold = builder.threshold;
        this.operator = OPERATORS.getOrDefault(builder.operator, "<=>");
        this.site = builder.site;
        this.language=validateLanguage(builder.language);
        this.searchOffset=builder.searchOffset;
        this.fields=builder.fields!=null ? builder.fields.trim().split("[\\s+,]") : new String[0];

    }

    String validateBuilderQuery(String query){
        if(UtilMethods.isEmpty(query)){
            throw new DotRuntimeException("query cannot be null");
        }
        return String.join(" ", query.trim().split("\\s+"));
    }
    long validateLanguage(String language) {
        return Try.of(()->Long.parseLong(language))
                .recover(x->APILocator.getLanguageAPI().getLanguage(language).getId())
                .getOrElseTry(()->APILocator.getLanguageAPI().getDefaultLanguage().getId());

    }


    public static final class Builder {
        @JsonSetter(nulls = Nulls.SKIP)
        private String query;

        @JsonSetter(nulls = Nulls.SKIP)
        private int searchLimit = 1000;

        @JsonSetter(nulls = Nulls.SKIP)
        private String language ;

        @JsonSetter(nulls = Nulls.SKIP)
        private int searchOffset = 0;

        @JsonSetter(nulls = Nulls.SKIP)
        private int responseLengthTokens = 1024;

        @JsonSetter(nulls = Nulls.SKIP)
        private boolean stream = false;

        @JsonProperty
        private String fieldVar;

        @JsonProperty
        private String indexName="default";

        @JsonProperty
        private String contentType;

        @JsonSetter(nulls = Nulls.SKIP)
        private float threshold = .25f;

        @JsonSetter(nulls = Nulls.SKIP)
        private String operator = "cosine";

        @JsonSetter(nulls = Nulls.SKIP)
        private String site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()).getIdentifier();

        @JsonSetter(nulls = Nulls.SKIP)
        public String fields;


        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder searchLimit(int searchLimit) {
            this.searchLimit = searchLimit;
            return this;
        }

        public Builder language(long language) {
            this.language = String.valueOf(language);
            return this;
        }
        public Builder language(String language) {
            this.language = String.valueOf(language);
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

        public CompletionsForm build() {
            return new CompletionsForm(this);

        }
    }
}
