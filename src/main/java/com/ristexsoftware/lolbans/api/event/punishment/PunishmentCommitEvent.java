package com.ristexsoftware.lolbans.api.event.punishment;

import com.ristexsoftware.lolbans.api.punishment.Punishment;

class PunishmentCommitEvent extends PunishmentEvent {
    public PunishmentCommitEvent(Punishment punishment) {
        super(punishment);
    }
}