package com.ristexsoftware.lolbans.api.event.punishment;

import com.ristexsoftware.lolbans.api.punishment.Punishment;

class PunishmentDeleteEvent extends PunishmentEvent {
    public PunishmentDeleteEvent(Punishment punishment) {
        super(punishment);
    }
}