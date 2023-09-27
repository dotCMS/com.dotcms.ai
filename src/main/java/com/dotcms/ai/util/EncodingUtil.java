package com.dotcms.ai.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import io.vavr.Lazy;

public class EncodingUtil {


    public static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public static final String model = ConfigProperties.getProperty("OPEN_AI_MODEL", ConfigProperties.getProperty("OPEN_AI_MODEL","text-embedding-ada-002"));

    public static Lazy<Encoding> encoding  = Lazy.of(()->
            registry.getEncodingForModel(model).get()
    );

}
