package com.dotcms.ai.util;


import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;

public class OpenAIRequest {

    private OpenAIRequest() {
    }



    public static String doRequest(String url, String method, String openAiAPIKey, String json) throws IOException, InterruptedException {


        final Map<String,String> headers = Map.of("Authorization", "Bearer " + openAiAPIKey , "Content-Type", "application/json");


        return CircuitBreakerUrl.builder()
                .setUrl(url)
                .setRawData(json)
                .setHeaders(headers)
                .setMethod(CircuitBreakerUrl.Method.POST)
                .setTimeout(60000)
                .build()
                .doString();






    }
    public static void doRequest(String url, String method, String openAiAPIKey, String json, OutputStream out) throws IOException, InterruptedException {


        final Map<String,String> headers = Map.of("Authorization", "Bearer " + openAiAPIKey , "Content-Type", "application/json");


        CircuitBreakerUrl.builder()
                .setUrl(url)
                .setRawData(json)
                .setHeaders(headers)
                .setMethod(CircuitBreakerUrl.Method.POST)
                .setTimeout(600000)
                .build()
                .doOut(out);






    }

}
