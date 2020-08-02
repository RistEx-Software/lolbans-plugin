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

package com.ristexsoftware.lolbans.api.punishment;

public enum PunishmentType
{
    // WARN: Do not change the ordering of these or
    // it will mix up types.
    BAN,
    MUTE,
    KICK,
    WARN,
    REGEX,
    IP,
    BANWAVE,
    UNKNOWN;

    /**
     * Get the display name of the PunishmentType
     * @return A human-readable string for the punishment type
     */
    public String displayName()
    {
        switch (this)
        {
            case BAN: return "Ban";
            case MUTE: return "Mute";
            case KICK: return "Kick";
            case WARN: return "Warning";
            case REGEX: return "Regex";
            case IP: return "IP Ban";
            case BANWAVE: return "Ban Wave";
            default:
                return "Unknown";
        }
    }

    /**
     * Get the display name of the PunishmentType
     * @return A human-readable string for the punishment type
     */
    public static String displayName(PunishmentType type)
    {
        switch (type)
        {
            case BAN: return "Ban";
            case MUTE: return "Mute";
            case KICK: return "Kick";
            case WARN: return "Warning";
            case REGEX: return "Regex";
            case IP: return "IP Ban";
            case BANWAVE: return "Ban Wave";
            default:
                return "Unknown";
        }
    }

    /**
     * Attempt to convert an integer to it's enum value
     * @param ordinal The integer value of the enum
     * @return the enum type of the punishment
     */
    public static PunishmentType fromOrdinal(int ordinal)
    {
        switch (ordinal)
        {
            case 0: return PunishmentType.BAN;
            case 1: return PunishmentType.MUTE;
            case 2: return PunishmentType.KICK;
            case 3: return PunishmentType.WARN;
            case 4: return PunishmentType.REGEX;
            case 5: return PunishmentType.IP;
            case 6: return PunishmentType.BANWAVE;
            default:
                return PunishmentType.UNKNOWN;
        }
    }
}