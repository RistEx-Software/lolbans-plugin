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
    @Getter String reportId;
    @Getter User target;
    @Getter @Setter String message;
    @Getter User reportedBy;
    @Getter Timestamp reportedAt;
    @Getter @Setter Boolean claimed;
    @Getter User claimedBy;
    @Getter Timestamp claimedAt;
    @Getter @Setter Boolean closed;
    @Getter @Setter PunishmentType resolution;
    @Getter @Setter String reference;

    public Report(
        String reportId,
        User target,
        String message,
        User reportedBy,
        Timestamp reportedAt,
        Boolean claimed,
        User claimedBy,
        Timestamp claimedAt,
        Boolean closed,
        PunishmentType resolution,
        String reference
    ) {
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