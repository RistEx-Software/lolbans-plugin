package com.ristexsoftware.lolbans.api.utils;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.common.utils.Debug;

import lombok.Getter;
import lombok.Setter;

public class Cache<T extends Cacheable> {
    public interface CacheTester<T extends Cacheable> {
        boolean match(T object);
    }

    private ConcurrentHashMap<String, T> objects = new ConcurrentHashMap<String, T>();
    private ConcurrentHashMap<String, Long> objectInsertionTimestamps = new ConcurrentHashMap<String, Long>();

    @Getter @Setter private Long ttl = (long) (30 * 60e3);

    private int memoryUsage = 0;
    @Getter @Setter
    private int maxMemoryUsage = 0;

    @Getter @Setter
    private int maxSize = 0;
    
    @Getter private FutureTask objectExpiryTask =  new FutureTask<>(new Callable<Boolean>() {
            @Override
        public Boolean call() {
                if (ttl <= 0) {
                    return false;
                }
                
                objectInsertionTimestamps.forEach((k, v) -> {
                    if (v + ttl < System.currentTimeMillis()) {
                        new Debug(Cache.class).print("Evicting " + k + " from " + clazz.getSimpleName() + " cache");
                        removeKey(k);
                    }
                });
                return true;
            }
        });


    private Class<T> clazz;
    public Cache(Class<T> clazz) {
        this.clazz = clazz;
    }

    private Debug debug = new Debug(getClass());

    /**
     * Return the size of this cache.
     */
    public int size() {
        return objects.size();
    }

    /**
     * Get the memory usage of this cache, specifying the units of the returned value.
     */
    public Double memoryUsage(MemoryUtil.Unit units) {
        return MemoryUtil.formatBits(memoryUsage, units);
    }   

    /**
     * Get the approximate memory usage of this cache in kilobytes.
     */
    public Double memoryUsage() {
        return memoryUsage(MemoryUtil.Unit.KILOBYTES);
    }

    /**
     * Retrieve an object from the cache.
     */
    public T get(String key) {
        debug.reset();
        T object = objects.get(key);
        
        if (object != null)
            debug.print("Got cached entry for " + clazz.getSimpleName() + " with key " + key);

        return object;
    }

    public Collection<T> getAll() {
        return objects.values();
    }

    /**
     * Return a boolean determining if the cache contains a value.
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Find an object using the given tester lambda.
     */
    public T find(CacheTester<T> tester) {
        debug.reset();
        for (T object : objects.values()) {
            if (tester.match(object)) {
                debug.print("Found cached entry for " + clazz.getSimpleName() + " with key " + object.getKey());
                return object;
            }
        }
        debug.print("Failed to find " + clazz.getSimpleName() + " using parsed matcher");
        return null;
    }

    /**
     * Store an object in the cache.
     */
    public void put(T object) {
        debug.reset();

        if (objects.containsKey(object.getKey())) {
            debug.print("Skipping insertion for " + clazz.getSimpleName() + " " + object.getKey() + " - already exists.");
            return;
        }

        if (maxSize > 0) {
            while (objects.size() >= maxSize) {
                removeOldestEntry();
            }
        }

        int size = MemoryUtil.getSizeOf(object);
        if (maxMemoryUsage > 0 && memoryUsage + size > maxMemoryUsage) {
            Runnable memoryReleaser = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        while (memoryUsage + size > maxMemoryUsage) {
                                // if (!isPunishmentCacheLowOnMemory) {
                                //     isPunishmentCacheLowOnMemory = true;
                                //     getPlugin().getLogger().warning(
                                //             "The punishment cache is running out of memory! It might be a good idea to add more in the plugin configuration.");
                                // }

                            removeOldestEntry();
                        }
                        memoryUsage += size;
                        debug.print("Released memory from " + clazz.getSimpleName() +" cache");
                    }
                }
            };
            LolBans.getPlugin().getPool().submit(memoryReleaser);
        }

        objects.put(object.getKey(), object);
        objectInsertionTimestamps.put(object.getKey(), System.currentTimeMillis());
        debug.print("Created cached entry for " + clazz.getSimpleName() + " with key " + object.getKey());
    }

    /**
     * Update a value in the cache - bypasses not-null check!
     */
    public void update(T object) {
        if (objects.containsKey(object.getKey())) {
            remove(object);
        }
        put(object);
    }

    /**
     * Remove an object from the cache, returning the old object.
     */
    public T remove(T object) {
        debug.reset();
        T didRemove = objects.remove(object.getKey());

        if (didRemove == null) {
            debug.print("Could not remove entry for " + clazz.getSimpleName() + " with key " + object.getKey() + " - does not exist");
            return null;
        }

        if (maxMemoryUsage > 0) {
            memoryUsage -= MemoryUtil.getSizeOf(object);
        }
        
        debug.print("Removed entry for " + clazz.getSimpleName() + " with key " + object.getKey());
        return didRemove;
    }

    public T removeKey(String key) {
        T object = objects.get(key);
        if (object == null) {
            return null;
        }

        return remove(object);
    }

    /**
     * Fetch the oldest entry in the cache.
     */
    public T getOldestEntry() {
        String oldest = null;
        Long oldestTimestamp = Long.MAX_VALUE;

        for (String k : objectInsertionTimestamps.keySet()) {
            Long v = objectInsertionTimestamps.get(k);
            if (v < oldestTimestamp) {
                oldest = k;
                oldestTimestamp = v;
            }
        }
        return objects.get(oldest);
    }

    /**
     * Remove the oldest entry from the cache.
     */
    public T removeOldestEntry() {
        return remove(getOldestEntry());
    }
}