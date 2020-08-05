package com.ristexsoftware.lolbans.common.utils;

import java.util.HashMap;
import java.util.UUID;

import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.LolBans;

import lombok.Getter;
import lombok.Setter;

public class CacheUtil {
    private static HashMap<UUID, User> users = new HashMap<UUID, User>();
    private static HashMap<UUID, Long> userCachedAtTimestamps = new HashMap<UUID, Long>();

    private static HashMap<String, Punishment> punishments = new HashMap<String, Punishment>();
    private static HashMap<String, Long> punishmentCachedAtTimestamps = new HashMap<String, Long>();

    @Getter @Setter private static int maxUserEntryCount = 0;

    @Getter @Setter private static int maxPunishmentEntryCount = 0;

    private static int userMemoryUsage = 0;
    private static int punishmentMemoryUsage = 0;

    @Getter @Setter private static int maxUserMemoryUsage = 0;
    @Getter @Setter private static int maxPunishmentMemoryUsage = 0;

    /**
     * How long cache entries should last until they are removed from existence
     * forever >:3
     */
    @Getter
    @Setter
    private static Long userTTL = (long) (24 * 60 * 60e3);

    @Getter
    @Setter
    private static Long punishmentTTL = (long) (12 * 60 * 60e3);

    private static boolean isUserCacheLowOnMemory = false;
    private static boolean isPunishmentCacheLowOnMemory = false;

    /**
     * The length of time between each check of the cache for expired user entries.
     */
    @Getter
    @Setter
    private static Long userExpiryRunnablePeriod = (long) (30 * 60e3);

    private static Runnable userExpiryRunnable = new Runnable() {
        @Override
        public void run() {
            userCachedAtTimestamps.forEach((k, v) -> {
                if (v + userTTL < System.currentTimeMillis()) {
                    removeUser(k);
                }
            });
        }
    };

    /**
     * The length of time between each check of the cache for expired punishment entries.
     */
    @Getter
    @Setter
    private static Long punishExpiryRunnablePeriod = (long) (30 * 60e3);

    private static Runnable punishExpiryRunnable = new Runnable() {
        @Override
        public void run() {
            punishmentCachedAtTimestamps.forEach((k, v) -> {
                if (v + punishmentTTL < System.currentTimeMillis()
                        || (punishments.get(k).getExpiresAt() != null ? System.currentTimeMillis()
                                : punishments.get(k).getExpiresAt().getTime())
                                + punishmentTTL < System.currentTimeMillis()
                        || !punishments.get(k).getAppealed()) {
                    removePunishment(k);
                }
            });
        }
    };

    /**
     * Get the size of this cache (number of entries).
     */
    public static int getCount() {
        return users.size() + punishments.size();
    }

    /**
     * Get the overall memory usage of the cache.
     */
    public static Double memoryUsage(MemoryUtil.Unit unit) {
        return MemoryUtil.formatBits(userMemoryUsage + punishmentMemoryUsage, unit);
    }

    /**
     * Return the size of the user cache.
     */
    public static int userEntryCount() {
        return users.size();
    }

    /**
     * Get the approximate memory usage of the user cache, defining the units of the returned value.
     */
    public static Double userMemoryUsage(MemoryUtil.Unit unit) {
        return MemoryUtil.formatBits(userMemoryUsage, unit);
    }

    /**
     * Get the approximate memory usage of the user cache in kilobytes.
     */
    public static Double userMemoryUsage() {
        return MemoryUtil.formatBits(userMemoryUsage, MemoryUtil.Unit.KILOBYTES);
    }

    /**
     * Get a user from the cache.
     * 
     * @return The cached user, if they exist.
     */
    public static User getUser(UUID uuid) {
        return users.get(uuid);
    }

    /**
     * Get a user from the cache using their username.
     */
    public static User getUser(String username) {
        for (User user : users.values()) {
            if (user.getName() == username) {
                return user;
            }
        }
        return null;
    }

    /**
     * Add a user to the cache.
     */
    public static void putUser(User user) {
        if (users.get(user.getUniqueId()) != null) {
            return;
        }
        


        if (maxUserEntryCount > 0) {
            while (users.size() >= maxUserEntryCount) {
                dropOldestUserEntry();
            }
        }

        if (maxUserMemoryUsage > 0) {
            int size = MemoryUtil.getSizeOf(user);

            if (!isUserCacheLowOnMemory) {
                isUserCacheLowOnMemory = true;
                LolBans.getLogger().warning(
                        "The user cache is running out of memory! It might be a good idea to add more in the plugin configuration.");
            }

            while (userMemoryUsage + size > maxUserMemoryUsage) {
                dropOldestUserEntry();
            }
            userMemoryUsage += size;
        }

        users.put(user.getUniqueId(), user);
        userCachedAtTimestamps.put(user.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Remove a user from the cache.
     */
    public static boolean removeUser(User user) {
        return removeUser(user.getUniqueId());
    }

    /**
     * Remove a user with the given UUID from the cache.
     */
    public static boolean removeUser(UUID uuid) {
        // Keep track of memory usage.
        if (maxUserMemoryUsage > 0) {
            userMemoryUsage -= MemoryUtil.getSizeOf(users.get(uuid));
        }

        return users.remove(uuid) != null && userCachedAtTimestamps.remove(uuid) != null;
    }

    /**
     * Get the oldest user entry in the cache.
     */
    public static User getOldestUserEntry() {
        UUID oldest = null;
        Long oldestTimestamp = Long.MAX_VALUE;

        for (UUID k : userCachedAtTimestamps.keySet()) {
            Long v = userCachedAtTimestamps.get(k);
            if (v < oldestTimestamp) {
                oldest = k;
                oldestTimestamp = v;
            }
        }
        return users.get(oldest);
    }

    /**
     * Drop the oldest user entry in the cache. WARNING: This is pretty intensive,
     * so only call it if you absolutely need to.
     */
    public static boolean dropOldestUserEntry() {
        return removeUser(getOldestUserEntry());
    }
    


    /**
     * Return the size of the user cache.
     */
    public static int punishmentEntryCount() {
        return punishments.size();
    }

    /**
     * Get the approximate memory usage of the punishment cache, defining the units of the returned value.
     */
    public static Double punishmentMemoryUsage(MemoryUtil.Unit unit) {
        return MemoryUtil.formatBits(punishmentMemoryUsage, unit);
    }

    /**
     * Get the approximate memory usage of the punishment cache in kilobytes.
     */
    public static Double punishmentMemoryUsage() {
        return MemoryUtil.formatBits(userMemoryUsage, MemoryUtil.Unit.KILOBYTES);
    }

    /**
     * Get a user from the cache.Add
     */
    public static Punishment getPunishment(String id) {
        return punishments.get(id);
    }

    /**
     * Add a punishment to the cache.
     */
    public static void putPunishment(Punishment punishment) {
        if (punishments.get(punishment.getPunishId()) != null) {
            return;
        }

        if (maxPunishmentEntryCount > 0) {
            while (punishments.size() >= maxPunishmentEntryCount) {
                dropOldestPunishmentEntry();
            }
        }

        if (maxPunishmentMemoryUsage > 0) {
            int size = MemoryUtil.getSizeOf(punishment);
            
            if (!isPunishmentCacheLowOnMemory) {
                isPunishmentCacheLowOnMemory = true;
                LolBans.getLogger().warning(
                        "The punishment cache is running out of memory! It might be a good idea to add more in the plugin configuration.");
            }

            while (punishmentMemoryUsage + size > maxPunishmentMemoryUsage) {
                dropOldestPunishmentEntry();
            }
            punishmentMemoryUsage += size;
        }

        punishments.put(punishment.getPunishId(), punishment);
        punishmentCachedAtTimestamps.put(punishment.getPunishId(), System.currentTimeMillis());
    }

    /**
     * Remove a punishment from the cache.
     */
    public static boolean removePunishment(Punishment punishment) {
        return removePunishment(punishment.getPunishId());
    }

    /**
     * Remove a punishment with the given ID from the cache.
     */
    public static boolean removePunishment(String id) {
        // Keep track of memory usage.
        if (maxPunishmentMemoryUsage > 0) {
            punishmentMemoryUsage -= MemoryUtil.getSizeOf(punishments.get(id));
        }

        return punishments.remove(id) != null && punishmentCachedAtTimestamps.remove(id) != null;
    }

    public static void clear() {
        users.clear();
        punishments.clear();

        userMemoryUsage = 0;
        punishmentMemoryUsage = 0;
    }

    /**
     * Get the oldest punishment entry in the cache.
     */
    public static Punishment getOldestPunishmentEntry() {
        String oldest = null;
        Long oldestTimestamp = Long.MAX_VALUE;

        for (String k : punishmentCachedAtTimestamps.keySet()) {
            Long v = punishmentCachedAtTimestamps.get(k);
            if (v < oldestTimestamp) {
                oldest = k;
                oldestTimestamp = v;
            }
        }
        return punishments.get(oldest);
    }

    /**
     * Drop the oldest punishment entry in the cache. WARNING: This is pretty
     * intensive, so only call it if you absolutely need to.
     */
    public static boolean dropOldestPunishmentEntry() {
        return removePunishment(getOldestPunishmentEntry());
    }
}