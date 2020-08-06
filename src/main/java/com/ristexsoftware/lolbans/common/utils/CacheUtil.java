package com.ristexsoftware.lolbans.common.utils;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.LolBans;

import lombok.Getter;
import lombok.Setter;

public class CacheUtil {
    @Getter private static HashMap<String, User> users = new HashMap<String, User>();
    private static HashMap<String, Long> userCachedAtTimestamps = new HashMap<String, Long>();

    @Getter private static HashMap<String, Punishment> punishments = new HashMap<String, Punishment>();
    private static HashMap<String, Long> punishmentCachedAtTimestamps = new HashMap<String, Long>();

    // private static HashMap<String, String[]> userPunishments = new HashMap<String, String[]>();

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

    @Getter
    private static Runnable userExpiryRunnable = new Runnable() {
        @Override
        public void run() {
            FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    userCachedAtTimestamps.forEach((k, v) -> {
                        if (v + userTTL < System.currentTimeMillis()) {
                            new Debug(CacheUtil.class).print("Evicting " + k + " from user cache");
                            removeUser(k);
                        }
                    });
                    return true;
                }
            });
    
            LolBans.pool.execute(t);
        }
    };

    /**
     * The length of time between each check of the cache for expired punishment entries.
     */
    @Getter
    @Setter
    private static Long punishExpiryRunnablePeriod = (long) (30 * 60e3);

    @Getter
    private static Runnable punishExpiryRunnable = new Runnable() {
        @Override
        public void run() {
            FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    punishmentCachedAtTimestamps.forEach((k, v) -> {
                        if (v + punishmentTTL < System.currentTimeMillis()
                                || (punishments.get(k).getExpiresAt() != null ? System.currentTimeMillis()
                                        : punishments.get(k).getExpiresAt().getTime())
                                        + punishmentTTL < System.currentTimeMillis()
                                || !punishments.get(k).getAppealed()) {
                            removePunishment(k);
                        }
                    });
                    return true;
                }
            });
    
            LolBans.pool.execute(t);
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
        return users.get(uuid.toString());
    }

    /**
     * Get a user from the cache using their username.
     */
    public static User getUser(String username) {
        Debug debug = new Debug(CacheUtil.class);

        for (User user : users.values()) {
            if (user.getName().equals(username)) {
                debug.print("Found cache entry for " + username + " (" + user.getUniqueId().toString() + ")");
                return user;
            }
        }
        return null;
    }

    /**
     * Add a user to the cache.
     */
    public static void putUser(User user) {
        Debug debug = new Debug(CacheUtil.class);

        if (users.get(user.getUniqueId().toString()) != null) {
            debug.print("Skipping cache entry creation for " + user.getName() + " (" + user.getUniqueId().toString() + ") - already exists");
            return;
        }

        if (maxUserEntryCount > 0) {
            while (users.size() >= maxUserEntryCount) {
                dropOldestUserEntry();
            }
        }

        if (maxUserMemoryUsage > 0) {
            Runnable userMemoryReleaser = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        // userExpiryRunnable.run();
                        int size = MemoryUtil.getSizeOf(user);
                        while (userMemoryUsage + size > maxUserMemoryUsage) {
                            if (!isUserCacheLowOnMemory) {
                                isUserCacheLowOnMemory = true;
                                LolBans.getLogger().warning(
                                        "The user cache is running out of memory! It might be a good idea to add more in the plugin configuration.");
                            }
                            dropOldestUserEntry();
                            userMemoryUsage += size;
                            debug.print("Released memory for user cache");
                        }
                    }
                }
            };
            debug.print("Submitted memory releaser runnable to executor pool");
            LolBans.pool.submit(userMemoryReleaser);
        }

        users.put(user.getUniqueId().toString(), user);
        userCachedAtTimestamps.put(user.getUniqueId().toString(), System.currentTimeMillis());
        debug.print("Created cache entry for " + user.getName() + " (" + user.getUniqueId().toString() + ")");
    }

    /**
     * Remove a user from the cache.
     */
    public static boolean removeUser(User user) {
        return removeUser(user.getUniqueId().toString());
    }

    /**
     * Remove a user with the given UUID from the cache.
     */
    public static boolean removeUser(UUID uuid) {
        return removeUser(uuid.toString());
    }

    /**
     * Remove a user with the given UUID in string format from the cache.
     */
    public static boolean removeUser(String uuid) {
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
        String oldest = null;
        Long oldestTimestamp = Long.MAX_VALUE;

        for (String k : userCachedAtTimestamps.keySet()) {
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
        Debug debug = new Debug(CacheUtil.class);
         
        if (punishments.get(punishment.getPunishId()) != null) {
            debug.print("Skipping cache entry creation for punishment " + punishment.getPunishId() + " - already exists");
            return;
        }

        if (maxPunishmentEntryCount > 0) {
            while (punishments.size() >= maxPunishmentEntryCount) {
                dropOldestPunishmentEntry();
            }
        }

        if (maxPunishmentMemoryUsage > 0) {
            Runnable punishmentMemoryReleaser = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        // punishExpiryRunnable.run();
                        int size = MemoryUtil.getSizeOf(punishment);

                        while (punishmentMemoryUsage + size > maxPunishmentMemoryUsage) {
                                if (!isPunishmentCacheLowOnMemory) {
                                    isPunishmentCacheLowOnMemory = true;
                                    LolBans.getLogger().warning(
                                            "The punishment cache is running out of memory! It might be a good idea to add more in the plugin configuration.");
                                }

                            dropOldestUserEntry();
                        }
                        punishmentMemoryUsage += size;
                        debug.print("Released memory from punishment cache");
                    }
                }
            };
            LolBans.pool.submit(punishmentMemoryReleaser);
            debug.print("Submitted memory releaser runnable to executor pool");
        }

        punishments.put(punishment.getPunishId(), punishment);
        punishmentCachedAtTimestamps.put(punishment.getPunishId(), System.currentTimeMillis());
        debug.print("Created cache entry for punishment " + punishment.getPunishId());
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