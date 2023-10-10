package com.dotcms.ai.util;


import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class OpenAIRequest {

    static final long OPENAI_RATE_LIMIT_PER_MINUTE = ConfigProperties.getIntProperty("OPENAI_RATE_LIMIT_PER_MINUTE", 60);
    static final long MIN_INTERVAL = 60000 / OPENAI_RATE_LIMIT_PER_MINUTE;
    static final AtomicLong lastRestCall = new AtomicLong();

    private OpenAIRequest() {
    }

    public static String doRequest(String url, String method, String openAiAPIKey, String json) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doRequest(url, method, openAiAPIKey, json, out);
        return out.toString();


    }

    public static void doRequest(String url, String method, String openAiAPIKey, String json, OutputStream out) throws IOException {


        final Map<String, String> headers = Map.of("Authorization", "Bearer " + openAiAPIKey, "Content-Type", "application/json");

        synchronized (OpenAIRequest.class) {
            try {
                long now = System.currentTimeMillis();
                long sleep = now - lastRestCall.get() >= MIN_INTERVAL ? 0 : lastRestCall.get() + MIN_INTERVAL - now;
                if (sleep > 0) {
                    Logger.info(OpenAIRequest.class, "Rate limit:" + OPENAI_RATE_LIMIT_PER_MINUTE + "/minute, or 1 every " + (60000 / OPENAI_RATE_LIMIT_PER_MINUTE) + "ms. Sleeping:" + sleep);
                    Try.run(() -> Thread.sleep(sleep));
                }

                CircuitBreakerUrl.builder().setUrl(url).setRawData(json).setHeaders(headers).setMethod(resolveMethod(method)).setTimeout(600000).build().doOut(out);
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



    public static void streamRequest(String urlIn, String openAiAPIKey, String json, OutputStream out) {

        System.out.println("posting:" + json);
        try {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                StringEntity jsonEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
                HttpPost httpPost = new HttpPost(urlIn);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + openAiAPIKey);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setEntity(jsonEntity);
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent());
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        out.flush();
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
