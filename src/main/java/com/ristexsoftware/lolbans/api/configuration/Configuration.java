/* 
 *  LolBans - The advanced banning system for Minecraft
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

// package com.ristexsoftware.lolbans.api.configuration;

// import java.io.File;    
// import java.nio.channels.FileChannel;
// import java.io.IOException;
// import java.util.InputMismatchException;
// import java.io.FileOutputStream;
// import java.io.FileInputStream;

// import org.bukkit.plugin.java.JavaPlugin;
// import net.md_5.bungee.api.plugin.Plugin;

// public class Configuration {

//     /**
//      * A ConfigType object pretty much just stores
//      * the file path of the config file. This prevents
//      * code dupe/hard coding withing the Config object
//      * 
//      * @param path The String path of the config
//      * 
//      * @see #Config
//      */
//     public enum ConfigType {
//         MAIN_CONFIG("/config.yml"),
//         LANG_CONFIG("/lang/messages.en_US.yml");
        
//         String p;
//         ConfigType(String path) {
//             this.p = path;
//         }

//         String getPath() {
//             return p;
//         }

//     }
    
//     private File conf;
//     private Object self;
//     private ConfigType t;

//     /**
//      * @param type The ConfigType (Provides config path)
//      * @param overwrite Whether an existing config should be overwritten
//      * @param plugin The plugin instance this config belongs to (Must be of type JavaPlugin or Plugin)
//      * 
//      * @see ConfigType
//      * @see org.bukkit.plugin.java.JavaPlugin JavaPlugin
//      * @see net.md_5.bungee.api.plugin.Plugin Plugin
//      */
//     public Configuration(ConfigType type, boolean overwrite, Object plugin) {
//         if(!(plugin instanceof JavaPlugin || plugin instanceof Plugin))
//             throw new InputMismatchException(String.valueOf(plugin.getClass()) + " is not a valid Plugin class");

//         String pluginPath = null;

//         if(plugin instanceof JavaPlugin)
//             self = (JavaPlugin) plugin;
//             pluginPath = ((JavaPlugin) self).getDataFolder().getPath();

//         if(plugin instanceof Plugin)
//             self = (Plugin) plugin;
//             pluginPath = ((Plugin) self).getDataFolder().getPath();

//         this.t = type;
//         ClassLoader classLoader = plugin.getClass().getClassLoader();
//         File f = new File(classLoader.getResource(type.getPath()).getPath());

//         conf = new File(pluginPath + type.getPath());
        
//         if(conf.exists() && overwrite) {
//             copyContents(f, conf);
//             return;
//         }

//         copyContents(f, conf);
//     }
    
//     /**
//      * @return Returns the ConfigType object
//      */
//     public ConfigType getType() {
//         return this.t; //stfu VSCode
//     }

//     /**
//      * @return Returns the plugin instance this config belongs to (may return JavaPlugin or Plugin)
//      * 
//      * @see org.bukkit.plugin.java.JavaPlugin JavaPlugin
//      * @see net.md_5.bungee.api.plugin.Plugin Plugin
//      */
//     public Object getInstance() {
//         return self;
//     }

//     void copyContents(File src, File dest) {
//         try {
//             // Thank you Java 
//             FileInputStream srcIN = new FileInputStream(src); 
//             FileOutputStream destOUT = new FileOutputStream(dest);

//             FileChannel srcCH = srcIN.getChannel();
//             FileChannel destCH = destOUT.getChannel();
//             destCH.transferFrom(srcCH, 0L, srcCH.size()); 

//             srcIN.close();
//             destOUT.close();

//         } catch(IOException e) {
//             System.out.println(e);
//         }
//     }
// }