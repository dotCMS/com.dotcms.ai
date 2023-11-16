package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AsyncWorkflowRunnerWrapper implements Runnable {
    private static final Set<String> RUNNING_CONTENT = ConcurrentHashMap.newKeySet();

    private final AsyncWorkflowRunner asyncWorkflowRunner;
    private final String contentletKey;
    private int rescheduled;

    AsyncWorkflowRunnerWrapper(AsyncWorkflowRunner runner) {
        this(runner, 0);

    }

    AsyncWorkflowRunnerWrapper(AsyncWorkflowRunner runner, int rescheduled) {
        this.asyncWorkflowRunner = runner;
        this.contentletKey = Try.of(() -> asyncWorkflowRunner.getIdentifier() + asyncWorkflowRunner.getLanguage()).getOrElseThrow(DotRuntimeException::new);
        if (UtilMethods.isEmpty(asyncWorkflowRunner.getIdentifier())) {
            throw new DotRuntimeException("Content must be saved before it can be run async - no identifier");
        }
        this.rescheduled=rescheduled;
    }

        @Override
        public void run() {
            boolean runningNow = false;
            try {
                runningNow = shouldRunNow();
                if (!runningNow) {
                    return;
                }
                LocalTransaction.wrap(asyncWorkflowRunner::runInternal);
                HibernateUtil.commitTransaction();
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

        synchronized boolean shouldRunNow() {
            if(!RUNNING_CONTENT.contains(contentletKey)){
                RUNNING_CONTENT.add(contentletKey);
                return true;
            }
            runLater();
            return false;
        }


        private void runLater() {
            if(rescheduled > 1000){
                RUNNING_CONTENT.remove(contentletKey);
                Logger.warn(this.getClass(), "Unable to schedule " + this.getClass().getSimpleName() + " for content id:" + contentletKey);
                Logger.warn(this.getClass(), "Unable to schedule " + this.getClass().getSimpleName() + " for content id:" + contentletKey);
                return;
            }
            OpenAIThreadPool.schedule(new AsyncWorkflowRunnerWrapper(asyncWorkflowRunner, ++rescheduled), 5, TimeUnit.SECONDS);
        }

    }
