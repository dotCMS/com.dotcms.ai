package com.dotcms.ai.util;

import com.dotmarketing.exception.DotRuntimeException;

public enum OpenAIModel {

    GPT_3_5_TURBO(3000, 3500, 4096),
    GPT_3_5_TURBO_INSTRUCT(250000, 3000, 16384),
    TEXT_EMBEDDING_ADA_002(1000000, 3000, 8191),
    GPT_4(10000, 200, 8191);


    public final int tokensPerMinute;
    public final int apiPerMinute;
    public final int maxTokens;

    OpenAIModel(int tokensPerMinute, int apiPerMinute, int maxTokens) {
        this.tokensPerMinute = tokensPerMinute;
        this.apiPerMinute = apiPerMinute;
        this.maxTokens = maxTokens;
    }

    public static OpenAIModel resolveModel(String modelIn) {
        String modelOut = modelIn.replace("-", "_").replace(".", "_").toUpperCase().trim();
        for (OpenAIModel openAiModel : OpenAIModel.values()) {
            if (openAiModel.name().equalsIgnoreCase(modelOut)) {
                return openAiModel;
            }
        }
        throw new DotRuntimeException("Unable to parse model:" + modelIn + ".  Only gpt-3.5-turbo, gpt-3.5-turbo-instruct, gpt-4 and text_embedding_ada_002 are supported ");

    }

    public long minIntervalBetweenCalls() {
        return 60000 / apiPerMinute;
    }

}
