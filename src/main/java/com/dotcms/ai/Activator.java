package com.dotcms.ai;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.listener.EmbeddingContentListener;
import com.dotcms.ai.rest.CompletionsResource;
import com.dotcms.ai.rest.EmbeddingsResource;
import com.dotcms.ai.rest.ImageResource;
import com.dotcms.ai.rest.SearchResource;
import com.dotcms.ai.rest.TextResource;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotcms.ai.viewtool.AIToolInfo;
import com.dotcms.ai.workflow.DotEmbeddingsActionlet;
import com.dotcms.ai.workflow.OpenAIContentPromptActionlet;
import com.dotcms.ai.workflow.OpenAIGenerateImageActionlet;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.languagevariable.business.LanguageVariableAPIImpl;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPIImpl;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Activator extends GenericBundleActivator {

    private static final EmbeddingContentListener LISTENER = new EmbeddingContentListener();
    private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() + File.separator + "server" + File.separator + "apps" + File.separator + AppKeys.APP_YAML_NAME);
    private Class[] clazzes = {
            TextResource.class,
            ImageResource.class,
            EmbeddingsResource.class,
            CompletionsResource.class,
            SearchResource.class
    };
    private final List<WorkFlowActionlet> actionlets = List.of(new DotEmbeddingsActionlet(), new OpenAIContentPromptActionlet(), new OpenAIGenerateImageActionlet());

    private LoggerContext pluginLoggerContext;

    public void start(BundleContext context) throws Exception {
        //Initializing log4j...
        LoggerContext dotcmsLoggerContext = Log4jUtil.getLoggerContext();
        //Initialing the log4j context of this plugin based on the dotCMS logger context
        pluginLoggerContext = (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(), false, dotcmsLoggerContext, dotcmsLoggerContext.getConfigLocation());


        //Registering the ViewTool service
        registerViewToolService(context, new AIToolInfo());

        // copy the yaml
        copyAppYml();

        // create language variables if they don't exist
        createLanguageVariables();


        // Register Embedding Actionlet
        actionlets.forEach(a->{
            this.registerActionlet(context, a);
        });


        //Initializing services...
        initializeServices(context);

        // Add the Embedding Listener (this does nothing right now)
        subscribeEmbeddingsListener();

        publishPortlet(context);


        for (Class clazz : clazzes) {
            Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
            RestServiceUtil.addResource(clazz);
        }


    }

    public void stop(BundleContext context) throws Exception {


        for (Class clazz : clazzes) {
            Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
            RestServiceUtil.removeResource(clazz);
        }
        OpenAIThreadPool.shutdown();

        //Unregister all the bundle services
        unregisterServices(context);

        unregisterViewToolServices();


        unsubscribeEmbeddingsListener();
        EmbeddingsAPI.impl(null).shutdown();
        deleteYml();
        //Shutting down log4j in order to avoid memory leaks
        Log4jUtil.shutdown(pluginLoggerContext);
    }

    private void unsubscribeEmbeddingsListener() {
        APILocator.getLocalSystemEventsAPI().unsubscribe(LISTENER);
    }

    /**
     * Deletes the App yaml to the apps directory and refreshes the apps
     */
    private void deleteYml() throws IOException {

        Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

        installedAppYaml.delete();
        CacheLocator.getAppsCache().clearCache();
    }

    /**
     * copies the App yaml to the apps directory and refreshes the apps
     */
    private void copyAppYml() throws IOException {

        Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);

        if (!installedAppYaml.exists()) {
            installedAppYaml.createNewFile();
        }

        try (final InputStream in = this.getClass().getResourceAsStream("/" + AppKeys.APP_YAML_NAME); final OutputStream out = Files.newOutputStream(installedAppYaml.toPath())) {
            IOUtils.copy(in, out);
        }

        CacheLocator.getAppsCache().clearCache();
    }

    public void createLanguageVariables() {
        Logger.info(this, "Creating test Language Variable contents...");

        // Get system user and required APIs


        LanguageAPI languageAPI = new LanguageAPIImpl();

        // Get the default language ID
        long languageId = languageAPI.getDefaultLanguage().getId();

        // Define key-value pairs in a map
        Map<String, String> langVariableMap = Map.of("ai-text-box-key", "AI text box value", "ai-text-area-key", "AI text area value");

        // Iterate through the map and create language variables
        langVariableMap.forEach((key, value) -> {
            // Check if the language variable already exists
            if (!languageVariableExists(key, languageId, APILocator.systemUser())) {
                // If not, create it
                try {
                    createLanguageVariable(key, value, languageId);
                } catch (DotDataException | DotSecurityException e) {
                    Logger.error(this.getClass(), "Error creating language variable: " + e.getMessage());
                }
            } else {
                Logger.info(this, "Language variable already exists for key: " + key);
            }
        });
    }

    private void subscribeEmbeddingsListener() {

        APILocator.getLocalSystemEventsAPI().subscribe(LISTENER);

    }

    private void publishPortlet(BundleContext context) throws Exception {

        // Add the test AI portlet
        registerPortlets(context, new String[]{"portlet.xml"});


        // force a reload for the velocity portlet
        CacheLocator.getVeloctyResourceCache().clearCache();

        createLanguageVariable(com.dotcms.repackage.javax.portlet.Portlet.class.getPackage().getName() + ".title.dotAI", "dotAI", APILocator.getLanguageAPI().getDefaultLanguage().getId());


        // Add language key
        final Map<String, String> keys = Map.of(
                com.dotcms.repackage.javax.portlet.Portlet.class.getPackage().getName() + ".title.dotAI",
                "dotAI");
        APILocator.getLanguageAPI().getLanguages().forEach(l -> {
            Try.run(() -> APILocator.getLanguageAPI().saveLanguageKeys(l, keys, new HashMap<>(), Set.of()));
        });


    }

    private boolean languageVariableExists(String key, long languageId, User systemUser) {
        LanguageVariableAPI languageVariableAPI = new LanguageVariableAPIImpl();
        String langVar = languageVariableAPI.getLanguageVariable(key, languageId, systemUser);
        return langVar != null && !langVar.equals(key);
    }

    private void createLanguageVariable(String key, String value, long languageId) throws DotDataException, DotSecurityException {

        if (languageVariableExists(key, languageId, APILocator.systemUser())) {
            return;
        }

        ContentType languageVariableContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE);
        Map<String, Field> fields = languageVariableContentType.fieldMap();

        Contentlet languageVariable = new Contentlet();

        // Set content properties for the language variable
        languageVariable.setContentTypeId(languageVariableContentType.inode());
        languageVariable.setLanguageId(languageId);

        Logger.info(this, "Creating AI Language Variable content: " + key);

        // Set key and value fields
        languageVariable.setStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
        languageVariable.setStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);

        // Set index policy and disable workflow
        languageVariable.setIndexPolicy(IndexPolicy.FORCE);
        languageVariable.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);

        // Check in the language variable
        languageVariable = APILocator.getContentletAPI().checkin(languageVariable, APILocator.systemUser(), Boolean.FALSE);

        // Publish the language variable
        languageVariable.setIndexPolicy(IndexPolicy.FORCE);
        languageVariable.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(languageVariable, APILocator.systemUser(), Boolean.FALSE);
        Logger.info(this, "Key/Value has been created for key: " + key);
    }


}
