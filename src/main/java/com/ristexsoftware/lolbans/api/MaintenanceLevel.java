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

package com.ristexsoftware.lolbans.api;

public enum MaintenanceLevel {
    LOWEST, LOW, HIGH, HIGHEST, UWU;

    public String displayName() {
        switch (this) {
            case LOWEST:
                return "Lowest";
            case LOW:
                return "Low";
            case HIGH:
                return "High";
            case HIGHEST:
                return "Highest";
            default:
                return "Unknown";
        }
    }

    public static String displayName(MaintenanceLevel level) {
        switch (level) {
            case LOWEST:
                return "Lowest";
            case LOW:
                return "Low";
            case HIGH:
                return "High";
            case HIGHEST:
                return "Highest";
            default:
                return "Unknown";
        }
    }

    public static MaintenanceLevel fromOrdinal(Integer level) {
        switch (level) {
            case 0:
                return LOWEST;
            case 1:
                return LOW;
            case 2:
                return HIGH;
            case 3:
                return HIGHEST;
            default:
                return HIGH;
        }
    }
}