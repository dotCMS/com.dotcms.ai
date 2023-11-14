package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncWorkflowRunnerWrapper implements Runnable {
    private static final Set<String> RUNNING_CONTENT = ConcurrentHashMap.newKeySet();
    private final long startTime;
    private final int runDelay;
    private final AsyncWorkflowRunner asyncWorkflowRunner;
    private final String contentletKey;


    AsyncWorkflowRunnerWrapper(AsyncWorkflowRunner runner) {
        this.runDelay = Try.of(() -> Integer.parseInt(runner.getParams().get("runDelay").getValue())).getOrElse(5);
        this.startTime = System.currentTimeMillis() + (runDelay * 1000L);
        this.asyncWorkflowRunner = runner;
        this.contentletKey = Try.of(() -> asyncWorkflowRunner.getProcessor().getContentlet().getIdentifier() + asyncWorkflowRunner.getProcessor().getContentlet().getLanguageId()).getOrElseThrow(DotRuntimeException::new);

    }

    synchronized boolean runningNow() {

        if (startTime > System.currentTimeMillis()) {
            runLater();
            return false;
        }

        if (RUNNING_CONTENT.contains(contentletKey)) {
            runLater();
            return false;
        }
        RUNNING_CONTENT.add(contentletKey);
        return true;
    }

    private void runLater() {
        Try.run(() -> Thread.sleep(1000));
        OpenAIThreadPool.threadPool().submit(this);
    }

    @Override
    public void run() {
        boolean runningNow = runningNow();
        try {
            if (!runningNow) {
                return;
            }
            LocalTransaction.wrap(asyncWorkflowRunner::runInternal);
            if (runDelay == 0) {
                HibernateUtil.commitTransaction();
            }
        } catch (
                Throwable e) { //NOSONAR -this catches throwable because if one is thrown, it destroys the whole thread pool.
            Logger.warn(this.getClass(), e.getMessage());
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this.getClass(), e.getMessage(), e);
            }
        } finally {
            if (runningNow) {
                RUNNING_CONTENT.remove(contentletKey);
            }
        }
    }

}
