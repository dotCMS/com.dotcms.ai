package com.dotcms.ai;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.viewtool.AIToolInfo;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.languagevariable.business.LanguageVariableAPIImpl;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPIImpl;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
        registerViewToolService(context, new AIToolInfo());

        // copy the yaml
        copyAppYml();

        // create language variables if they don't exist
        createLanguageVariables();
    }

    public void createLanguageVariables() {
        Logger.info(this, "Creating test Language Variable contents...");

        // Get system user and required APIs
        User systemUser = APILocator.systemUser();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        LanguageAPI languageAPI = new LanguageAPIImpl();

        // Get the default language ID
        int languageId = (int) languageAPI.getDefaultLanguage().getId();

        // Define key-value pairs in a map
        HashMap<String, String> langVariableMap = new HashMap<>();
        langVariableMap.put("ai-text-box-key", "AI text box value");
        langVariableMap.put("ai-text-area-key", "AI text area value");

        // Iterate through the map and create language variables
        langVariableMap.forEach((key, value) -> {
            // Check if the language variable already exists
            if (!languageVariableExists(key, languageId, systemUser)) {
                // If not, create it
                try {
                    createLanguageVariable(key, value, languageId, contentletAPI, systemUser);
                } catch (DotDataException | DotSecurityException e) {
                    Logger.error(this.getClass(), "Error creating language variable: " + e.getMessage() );
                }
            } else {
                Logger.info(this, "Language variable already exists for key: " + key);
            }
        });
    }

    private boolean languageVariableExists(String key, int languageId, User systemUser) {
        LanguageVariableAPI languageVariableAPI = new LanguageVariableAPIImpl();
        String langVar = languageVariableAPI.getLanguageVariable(key, languageId, systemUser);
        return langVar != null && !langVar.equals(key);
    }

    private void createLanguageVariable(String key, String value, int languageId, ContentletAPI contentletAPI, User systemUser)
        throws DotDataException, DotSecurityException {
        ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);
        Map<String, Field> fields = languageVariableContentType.fieldMap();

        Contentlet languageVariable = new Contentlet();

        // Set content properties for the language variable
        languageVariable.setContentTypeId(languageVariableContentType.inode());
        languageVariable.setLanguageId(languageId);

        Logger.info(this, "Creating AI Language Variable content: " + key);

        // Set key and value fields
        contentletAPI.setContentletProperty(languageVariable, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)), key);
        contentletAPI.setContentletProperty(languageVariable, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)), value);

        // Set index policy and disable workflow
        languageVariable.setIndexPolicy(IndexPolicy.FORCE);
        languageVariable.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);

        // Check in the language variable
        languageVariable = contentletAPI.checkin(languageVariable, systemUser, Boolean.FALSE);

        // Publish the language variable
        languageVariable.setIndexPolicy(IndexPolicy.FORCE);
        languageVariable.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentletAPI.publish(languageVariable, systemUser, Boolean.FALSE);
        Logger.info(this, "Key/Value has been created for key: " + key);
    }

    public static com.dotmarketing.portlets.structure.model.Field asOldField(final Field newField) {
        return new LegacyFieldTransformer(newField).asOldField();
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