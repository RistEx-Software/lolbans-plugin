/* 
 *  LolBans - The advanced banning system for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  Copyright (C) 2019-2020 Michael <md_5@spigotmc.org>
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

package com.ristexsoftware.lolbans.api.configuration;

import java.util.Map;


/**
 * This is a {@link Configuration} implementation that does not save or load
 * from any source, and stores all values in memory only.
 * This is useful for temporary Configurations for providing defaults.
 */
public class MemoryConfiguration extends MemorySection implements Configuration {
    protected Configuration defaults;
    protected MemoryConfigurationOptions options;

    /**
     * Creates an empty {@link MemoryConfiguration} with no default values.
     */
    public MemoryConfiguration() {}

    /**
     * Creates an empty {@link MemoryConfiguration} using the specified {@link
     * Configuration} as a source for all default values.
     *
     * @param defaults Default value provider
     * @throws IllegalArgumentException Thrown if defaults is null
     */
    public MemoryConfiguration( Configuration defaults) {
        this.defaults = defaults;
    }

    @Override
    public void addDefault(String path,  Object value) {
        // Validate.notNull(path, "Path may not be null");

        if (defaults == null) {
            defaults = new MemoryConfiguration();
        }

        defaults.set(path, value);
    }

    @Override
    public void addDefaults(Map<String, Object> defaults) {
        // Validate.notNull(defaults, "Defaults may not be null");

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            addDefault(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void addDefaults(Configuration defaults) {
        // Validate.notNull(defaults, "Defaults may not be null");

        addDefaults(defaults.getValues(true));
    }

    @Override
    public void setDefaults(Configuration defaults) {
        // Validate.notNull(defaults, "Defaults may not be null");

        this.defaults = defaults;
    }

    @Override
    
    public Configuration getDefaults() {
        return defaults;
    }

    
    @Override
    public ConfigurationSection getParent() {
        return null;
    }

    @Override
    
    public MemoryConfigurationOptions options() {
        if (options == null) {
            options = new MemoryConfigurationOptions(this);
        }

        return options;
    }
}
