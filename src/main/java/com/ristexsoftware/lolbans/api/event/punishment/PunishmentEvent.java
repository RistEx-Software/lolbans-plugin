package com.ristexsoftware.lolbans.api.event.punishment;

import com.ristexsoftware.lolbans.api.event.Event;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.User;

import lombok.Getter;

/**
 * Represents an event related to punishments.
 */
public class PunishmentEvent extends Event {
    @Getter
    private Punishment punishment;

    @Getter
    private User target;

    @Getter
    private User punisher;

    @Getter
    private User appealedBy;
    
    public PunishmentEvent(Punishment punishment) {
        this.punishment = punishment;
        this.target = punishment.getTarget();
        this.punisher = punishment.getPunisher();
        this.appealedBy = punishment.getAppealedBy();
    }
}