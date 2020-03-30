package com.ristexsoftware.lolbans.Utils;

public enum PunishmentType
{
    // WARN: Do not change the ordering of these or
    // it will mix up types.
    PUNISH_BAN,
    PUNISH_MUTE,
    PUNISH_KICK,
    PUNISH_WARN,
    PUNISH_UNKNOWN;

    public String DisplayName()
    {
        switch (this)
        {
            case PUNISH_BAN: return "Ban";
            case PUNISH_MUTE: return "Mute";
            case PUNISH_KICK: return "Kick";
            case PUNISH_WARN: return "Warning";
        }

        return "Unknown";
    }

    public static PunishmentType FromOrdinal(int ordinal)
    {
        switch (ordinal)
        {
            case 0: return PunishmentType.PUNISH_BAN;
            case 1: return PunishmentType.PUNISH_MUTE;
            case 2: return PunishmentType.PUNISH_KICK;
            case 3: return PunishmentType.PUNISH_WARN;
        }
        return PunishmentType.PUNISH_UNKNOWN;
    }
}