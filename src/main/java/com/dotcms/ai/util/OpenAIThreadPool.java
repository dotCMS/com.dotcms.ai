package com.dotcms.ai.util;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenAIThreadPool {


    private static ExecutorService service;
    private static AtomicBoolean running = new AtomicBoolean(true);


    public static final ExecutorService threadPool() {

        if (running.get() && service != null && !service.isShutdown() && !service.isTerminated()) {
            return service;
        }

        synchronized (OpenAIThreadPool.class) {
            if (running.get() && service != null && !service.isShutdown() && !service.isTerminated()) {
                return service;
            }
            int threads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS);
            int maxThreads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS_MAX);
            int queue = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS_QUEUE);
            service = new ThreadPoolExecutor(threads, maxThreads, 20000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queue), new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return service;
    }




    public static final void shutdown() {
        if(!running.getAndSet(false)){
            return;
        }
        service.shutdown();
    }

}
