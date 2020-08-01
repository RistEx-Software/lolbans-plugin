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

package com.ristexsoftware.lolbans.api.configuration.serialization;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an "alias" that a {@link ConfigurationSerializable} may be
 * stored as.
 * If this is not present on a {@link ConfigurationSerializable} class, it
 * will use the fully qualified name of the class.
 * <p>
 * This value will be stored in the configuration so that the configuration
 * deserialization can determine what type it is.
 * <p>
 * Using this annotation on any other class than a {@link
 * ConfigurationSerializable} will have no effect.
 *
 * @see ConfigurationSerialization#registerClass(Class, String)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableAs {
    /**
     * This is the name your class will be stored and retrieved as.
     * <p>
     * This name MUST be unique. We recommend using names such as
     * "MyPluginThing" instead of "Thing".
     *
     * @return Name to serialize the class as.
     */
    public String value();
}
