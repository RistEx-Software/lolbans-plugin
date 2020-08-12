package com.ristexsoftware.lolbans.api.event.punishment;

import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.User;

import lombok.Getter;

class PunishmentAppealEvent extends PunishmentEvent {
    public PunishmentAppealEvent(Punishment punishment) {
        super(punishment);
    }
}