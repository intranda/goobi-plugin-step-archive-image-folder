package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import io.goobi.workflow.api.connection.SftpUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ArchiveimagefolderStepPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = -2122931823980518591L;

    @Getter
    private String title = "intranda_step_archiveimagefolder";
    @Getter
    private Step step;
    private String sshUser;
    private String privateKeyLocation;
    private String privateKeyPassphrase;
    private String sshHost;
    private String knownHostsFile;
    private int port;
    private boolean deleteAndCloseAfterCopy;
    private String selectedImageFolder;
    private String returnPath;
    private SubnodeConfiguration methodConfig;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        sshUser = myconfig.getString("method/user", "intranda");
        privateKeyLocation = myconfig.getString("method/privateKeyLocation");
        privateKeyPassphrase = myconfig.getString("method/privateKeyPassphrase");
        sshHost = myconfig.getString("method/host", "intranda");
        port = myconfig.getInt("method/port", 22);
        knownHostsFile = myconfig.getString("method/knownHostsFile");
        selectedImageFolder = myconfig.getString("folder", "master");
        deleteAndCloseAfterCopy = myconfig.getBoolean("deleteAndCloseAfterCopy", false);
        methodConfig = myconfig.configurationAt("method");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_archiveimagefolder.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null; //NOSONAR
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = true;

        Path localFolder = null;
        int uploadedFiles = 0;
        try (SftpUtils sftpClient = new SftpUtils(sshUser, privateKeyLocation, privateKeyPassphrase, sshHost, port, knownHostsFile)) {

            localFolder = Paths.get(step.getProzess().getConfiguredImageFolder(selectedImageFolder));
            String folderName = localFolder.getFileName().toString();
            String remoteFolder = Paths.get(step.getProcessId().toString(), "images", folderName).toString();
            String[] folder = remoteFolder.split("/");
            for (String f : folder) {
                sftpClient.createSubFolder(f);
                sftpClient.changeRemoteFolder(f);
            }
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(localFolder)) {
                for (Path file : dirStream) {
                    sftpClient.uploadFile(file);
                    uploadedFiles++;
                }
            }
        } catch (IOException | SwapException | DAOException e) {
            log.error(e);
            Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR, "Error uploading files", title);
            successful = false;
        }

        log.debug("Archiveimagefolder step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        if (deleteAndCloseAfterCopy) {
            //save the method config to the folder
            methodConfig.setProperty("numberOfImages", uploadedFiles);
            XMLConfiguration xmlConf = new XMLConfiguration(methodConfig);
            try {
                xmlConf.save(localFolder.getParent().resolve(localFolder.getFileName().toString() + ".xml").toFile());
            } catch (ConfigurationException e) {
                log.error(e);
                Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR, "Error saving archive information to images folder", title);
                return PluginReturnValue.ERROR;
            }
            if (localFolder != null) {
                FileUtils.deleteQuietly(localFolder.toFile());
            }
            return PluginReturnValue.FINISH;
        }
        return PluginReturnValue.WAIT;
    }
}
