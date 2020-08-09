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

package com.ristexsoftware.lolbans.common.utils;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;

import lombok.Getter;

public class Timing {
    private Long start = System.currentTimeMillis();
    private Long later = 0L;
    @Getter
    private Long time;

    public Timing() {
    }

    public void finish() {
        later = System.currentTimeMillis();
        time = later - start;
    }

    public void finish(User sender) {
        try {
            finish();
            if (sender.isOnline()) {
                sender.sendMessage(
                        Messages.translate("command-complete", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                            {
                                put("Milliseconds", Long.toString(time));
                            }
                        }));
            }
        } catch (InvalidConfigurationException ex) {
            ex.printStackTrace();
        }
    }
}