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

import java.util.Random;
import com.ristexsoftware.lolbans.api.LolBans;

import lombok.Getter;

public class Debug {
    private Long start = System.nanoTime();
    @Getter private Long time;
    private int count = 0;
    private Class<?> clazz;
    Random r = new Random();
    String alphabet = "3569abcde";
    String randomcolor = "\u00A7"+alphabet.charAt(r.nextInt(alphabet.length()));
    public Debug(Class<?> claz) {
        this.clazz = claz;
    }

    public void print(Object message) {
        if (LolBans.getPlugin().getConfig().getBoolean("general.debug"))
            LolBans.getLogger().info(randomcolor + "(" + ++count + ") " + "("+clazz.getSimpleName()+".class) \u00A7r" + message + " | " + ((System.nanoTime() - start) / 1e3) + "μ");
    }

    /**
     * Reset this debug instance.
     */
    public void reset() {
        start = System.nanoTime();
        count = 0;
    }
}