package com.dotcms.ai.util;


import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class OpenAIRequest {

    final static long OPENAI_RATE_LIMIT_PER_MINUTE = ConfigProperties.getIntProperty("OPENAI_RATE_LIMIT_PER_MINUTE", 60);
    final static long MIN_INTERVAL = 60000 / OPENAI_RATE_LIMIT_PER_MINUTE;
    final static AtomicLong lastRestCall = new AtomicLong();

    private OpenAIRequest() {
    }

    public static String doRequest(String url, String method, String openAiAPIKey, String json) throws IOException, InterruptedException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doRequest(url, method, openAiAPIKey, json, out);
        return out.toString();


    }

    public static void doRequest(String url, String method, String openAiAPIKey, String json, OutputStream out) throws IOException, InterruptedException {


        final Map<String, String> headers = Map.of("Authorization", "Bearer " + openAiAPIKey, "Content-Type", "application/json");

        synchronized (OpenAIRequest.class) {
            try {
                long now = System.currentTimeMillis();
                long sleep = now - lastRestCall.get() >= MIN_INTERVAL ? 0 : lastRestCall.get() + MIN_INTERVAL - now;
                if (sleep > 0) {
                    Logger.info(OpenAIRequest.class, "Rate limit:" + OPENAI_RATE_LIMIT_PER_MINUTE + "/minute, or 1 every " + (60000 / OPENAI_RATE_LIMIT_PER_MINUTE)
                            + "ms. Sleeping:" + sleep);
                    Try.run(() -> Thread.sleep(sleep));
                }

                CircuitBreakerUrl.builder()
                        .setUrl(url)
                        .setRawData(json)
                        .setHeaders(headers)
                        .setMethod(resolveMethod(method))
                        .setTimeout(600000)
                        .build()
                        .doOut(out);
            } finally {
                lastRestCall.set(System.currentTimeMillis());
            }
        }


    }


    private static CircuitBreakerUrl.Method resolveMethod(String method) {
        if ("post" .equalsIgnoreCase(method)) {
            return CircuitBreakerUrl.Method.POST;
        }
        if ("put" .equalsIgnoreCase(method)) {
            return CircuitBreakerUrl.Method.PUT;
        }
        if ("delete" .equalsIgnoreCase(method)) {
            return CircuitBreakerUrl.Method.DELETE;
        }
        if ("patch" .equalsIgnoreCase(method)) {
            return CircuitBreakerUrl.Method.PATCH;
        }

        return CircuitBreakerUrl.Method.GET;

    }


}
