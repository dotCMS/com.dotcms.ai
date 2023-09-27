package com.dotcms.ai.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import com.dotcms.ai.util.Constants;
import org.apache.commons.io.IOUtils;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class AppUtil {
    private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() + File.separator + "server"
                    + File.separator + "apps" + File.separator + Constants.EMBEDDINGS_API_KEY + ".yml");

    /**
     * copies the App yaml to the apps directory and refreshes the apps
     * 
     * @throws IOException
     */
    public void copyAppYml() throws IOException {


        Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);
        try (final InputStream in = this.getClass().getResourceAsStream("/" + Constants.EMBEDDINGS_API_KEY + ".yml")) {
            IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
        }
        CacheLocator.getAppsCache().clearCache();


    }

    /**
     * Deletes the App yaml to the apps directory and refreshes the apps
     * 
     * @throws IOException
     */
    public void deleteYml() throws IOException {


        Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

        installedAppYaml.delete();
        CacheLocator.getAppsCache().clearCache();


    }



}
