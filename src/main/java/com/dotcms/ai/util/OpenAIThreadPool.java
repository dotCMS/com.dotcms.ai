package com.dotcms.ai.util;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import io.vavr.Lazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpenAIThreadPool {

    public static final Lazy<ExecutorService> threadPool = Lazy.of(() -> {

        int threads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS);
        int maxThreads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS_MAX);
        int queue = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS_QUEUE);
        return new ThreadPoolExecutor(threads, maxThreads, 20000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queue), new ThreadPoolExecutor.CallerRunsPolicy());

    });





}
