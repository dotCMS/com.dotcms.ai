package com.dotcms.ai.util;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

public class Logger {


    public static void warn(Class clazz, String message, Throwable e) {
        warn(clazz.getCanonicalName(), message, e);
    }

    public static void warn(String clazz, String message, Throwable e) {
        if (isDebug()) {
            com.dotmarketing.util.Logger.warn(clazz, message, e);
        } else {
            com.dotmarketing.util.Logger.warn(clazz, message);
        }
    }

    private static boolean isDebug() {
        return Try.of(() -> ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)).getOrElse(false);
    }

    public static void warn(Class clazz, String message) {
        warn(clazz.getCanonicalName(), message);
    }

    public static void warn(String clazz, String message) {
        warn(clazz, message, new DotRuntimeException(message));
    }

    public static void warn(Class clazz, Throwable e) {
        warn(clazz.getCanonicalName(), e.getMessage(), e);
    }

    public static void warn(String clazz, Throwable e) {
        warn(clazz, e.getMessage(), e);
    }

    public static void error(Class clazz, Throwable e) {
        error(clazz, e.getMessage(), e);
    }

    public static void error(Class clazz, String message, Throwable e) {
        error(clazz.getCanonicalName(), message, e);
    }

    public static void error(String clazz, String message, Throwable e) {
        if (isDebug()) {
            com.dotmarketing.util.Logger.error(clazz, message, e);
        } else {
            com.dotmarketing.util.Logger.error(clazz, message);
        }
    }

    public static void error(String clazz, Throwable e) {
        error(clazz, e.getMessage(), e);
    }

    public static void info(Class clazz, String message) {
        info(clazz.getCanonicalName(), message);
    }

    public static void info(String clazz, String message) {
        com.dotmarketing.util.Logger.info(clazz, message);
    }

    public static void debug(Class clazz, String message) {
        debug(clazz.getCanonicalName(), message);
    }

    public static void debug(String clazz, String message) {

        if (isDebug()) {
            com.dotmarketing.util.Logger.info(clazz, message);
        } else {
            com.dotmarketing.util.Logger.debug(clazz, message);
        }
    }


}
