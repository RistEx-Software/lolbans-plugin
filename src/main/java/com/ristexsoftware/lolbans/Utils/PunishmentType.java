/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Utils;

public enum PunishmentType
{
    // WARN: Do not change the ordering of these or
    // it will mix up types.
    PUNISH_BAN,
    PUNISH_MUTE,
    PUNISH_KICK,
    PUNISH_WARN,
    PUNISH_REGEX,
    PUNISH_IP,
    PUNISH_UNKNOWN;

    /**
     * Get the display name of the PunishmentType
     * @return A human-readable string for the punishment type
     */
    public String DisplayName()
    {
        switch (this)
        {
            case PUNISH_BAN: return "Ban";
            case PUNISH_MUTE: return "Mute";
            case PUNISH_KICK: return "Kick";
            case PUNISH_WARN: return "Warning";
            case PUNISH_REGEX: return "Regex";
            case PUNISH_IP: return "IP Ban";
            default:
                return "Unknown";
        }
    }

    /**
     * Get the display name of the PunishmentType
     * @return A human-readable string for the punishment type
     */
    public static String DisplayName(PunishmentType type)
    {
        switch (type)
        {
            case PUNISH_BAN: return "Ban";
            case PUNISH_MUTE: return "Mute";
            case PUNISH_KICK: return "Kick";
            case PUNISH_WARN: return "Warning";
            case PUNISH_REGEX: return "Regex";
            case PUNISH_IP: return "IP Ban";
            default:
                return "Unknown";
        }
    }

    /**
     * Attempt to convert an integer to it's enum value
     * @param ordinal The integer value of the enum
     * @return the enum type of the punishment
     */
    public static PunishmentType FromOrdinal(int ordinal)
    {
        switch (ordinal)
        {
            case 0: return PunishmentType.PUNISH_BAN;
            case 1: return PunishmentType.PUNISH_MUTE;
            case 2: return PunishmentType.PUNISH_KICK;
            case 3: return PunishmentType.PUNISH_WARN;
            case 4: return PunishmentType.PUNISH_REGEX;
            case 5: return PunishmentType.PUNISH_IP;
            default:
                return PunishmentType.PUNISH_UNKNOWN;
        }
    }
}