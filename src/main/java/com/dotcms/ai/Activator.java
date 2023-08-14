package com.dotcms.ai;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.viewtool.AIToolInfo;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    Class clazz = DotAIResource.class;

    private LoggerContext pluginLoggerContext;

    private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() + File.separator + "server"
        + File.separator + "apps" + File.separator + AppKeys.APP_YAML_NAME);

    public void start(BundleContext context) throws Exception {
        //Initializing log4j...
        LoggerContext dotcmsLoggerContext = Log4jUtil.getLoggerContext();
        //Initialing the log4j context of this plugin based on the dotCMS logger context
        pluginLoggerContext = (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(),
            false,
            dotcmsLoggerContext,
            dotcmsLoggerContext.getConfigLocation());

        //Initializing services...
        initializeServices(context);

        Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
        RestServiceUtil.addResource(clazz);

        //Registering the ViewTool service
        registerViewToolService( context, new AIToolInfo() );

        // copy the yaml
        copyAppYml();

    }

    public void stop(BundleContext context) throws Exception {

        deleteYml();

        Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
        RestServiceUtil.removeResource(clazz);

        //Unregister all the bundle services
        unregisterServices(context);

        unregisterViewToolServices();

        //Shutting down log4j in order to avoid memory leaks
		Log4jUtil.shutdown(pluginLoggerContext);

    }

    /**
     * copies the App yaml to the apps directory and refreshes the apps
     */
    private void copyAppYml() throws IOException {

        Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);

        if (!installedAppYaml.exists()) {
            installedAppYaml.createNewFile();
        }

        try (final InputStream in = this.getClass().getResourceAsStream("/" + AppKeys.APP_YAML_NAME)) {
            IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
        }

        CacheLocator.getAppsCache().clearCache();
    }

    /**
     * Deletes the App yaml to the apps directory and refreshes the apps
     */
    private void deleteYml() throws IOException {

        Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

        installedAppYaml.delete();
        CacheLocator.getAppsCache().clearCache();
    }
}