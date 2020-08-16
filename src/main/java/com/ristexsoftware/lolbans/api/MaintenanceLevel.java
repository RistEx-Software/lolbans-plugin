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