package com.dotcms.embeddings.util;


import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;

public class OpenAIRequest {

    private OpenAIRequest() {
    }

    private final static String[] headers = new String[]{"Authorization", "Bearer " + ConfigProperties.getProperty("OPEN_AI_API_KEY"), "Content-Type", "application/json"};


    public static String doRequest(String url, String method, String json) throws IOException, InterruptedException {

        //System.setProperty("jdk.httpclient.HttpClient.log","all");

        HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(2)).headers(headers);
        if ("post".equalsIgnoreCase(method)) {

            builder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, Charset.defaultCharset()));
        }


        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
        int responseCode = 200;
        String responseString;
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            responseCode = response.statusCode();
            return response.body();
        } catch (Exception e) {
            Logger.warn(OpenAIRequest.class, "Response :" + responseCode);
            Logger.warnAndDebug(OpenAIRequest.class, e);
            return null;
        }


    }


}
