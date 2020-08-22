/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import com.ristexsoftware.knappy.configuration.file.FileConfiguration;
import com.ristexsoftware.lolbans.api.LolBans;

import com.google.common.base.Charsets;
import lombok.Getter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConfigProvider {

    
    /**
     * Get the data folder in which config should be stored.
     */
    public File getDataFolder();
    
    /**
     * Fetch the config object itself.
     */
    public FileConfiguration getConfig();
    
    /**
     * Save the configuration to disk.
     */
    public void saveConfig();
    
    /**
     * Discards any data in getConfig() and reloads from disk.
     */
    public void reloadConfig();
    
    /**
     * Fetch the class loader.
     */
    public default ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }
    
    /**
     * Get the file configuration is stored in.
     */
    public default File getConfigFile() {
        return new File(getDataFolder(), "config.yml");
    }
    
    /**
     * Provides a reader for a text file located inside the jar.
     * <p>
     * The returned reader will read text with the UTF-8 charset.
     *
     * @param file the filename of the resource to load
     * @return null if {@link #getResource(String)} returns null
     * @throws IllegalArgumentException if file is null
     * @see ClassLoader#getResourceAsStream(String)
     */
    public default InputStreamReader getTextResource(String file) {
        final InputStream in = getResource(file);
        return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
    }

    /**
     * Return a boolean determining if the data folder exists. :3c
     */
    public default boolean dataFolderExists() {
        return getDataFolder().exists();
    }
    
    /**
     * Return whether the configuration file exists.
     */
    public default boolean configExists() {
        return getConfigFile().exists();
    }
    
    
    /**
     * Saves the default configuration for LolBans.
     */
    public default void saveDefaultConfig() {   
        saveResource("config.yml", false);
    }
    
    /**
     * Read an internal JAR resource file.
     */
    public default InputStream getResource(@NotNull String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        
        try {
            URL url = getClassLoader().getResource(filename);

            if (url == null) 
            return null;
            
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
    
    /**
     * Save an internal resource to the data file.
     */
    public default void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }
        
        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }
        
        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
        
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                LolBans.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            LolBans.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }
}