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

import java.sql.Timestamp;

import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.utils.PunishID;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a report sent by a user.
 */
public class Report {
    @Getter
    String reportId;
    @Getter
    User target;
    @Getter
    @Setter
    String message;
    @Getter
    User reportedBy;
    @Getter
    Timestamp reportedAt;
    @Getter
    @Setter
    Boolean claimed;
    @Getter
    User claimedBy;
    @Getter
    Timestamp claimedAt;
    @Getter
    @Setter
    Boolean closed;
    @Getter
    @Setter
    PunishmentType resolution;
    @Getter
    @Setter
    String reference;

    public Report(String reportId, User target, String message, User reportedBy, Timestamp reportedAt, Boolean claimed,
            User claimedBy, Timestamp claimedAt, Boolean closed, PunishmentType resolution, String reference) {
        this.reportId = reportId;
        this.target = target;
        this.message = message;
        this.reportedBy = reportedBy;
        this.reportedAt = reportedAt;
        this.claimed = claimed;
        this.claimedBy = claimedBy;
        this.claimedAt = claimedAt;
        this.closed = closed;
        this.resolution = resolution;
        this.reference = reference;
    }

    private static int INCREMENT = 0;

    /**
     * Create a new report, setting the ID and creation times automatically.
     */
    public Report(User target, String message, User reportedBy) {
        this(PunishID.generateID(INCREMENT++), target, message, reportedBy, TimeUtil.now(), false, null, null, false, null, null);
    }

}