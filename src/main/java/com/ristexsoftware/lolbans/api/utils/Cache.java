package com.ristexsoftware.lolbans.api.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.common.utils.Debug;

import lombok.Getter;
import lombok.Setter;

public class Cache<T extends Cacheable> {
    public interface CacheTester<T extends Cacheable> {
        boolean match(T object);
    }

    private HashMap<String, T> objects = new HashMap<String, T>();
    private HashMap<String, Long> objectInsertionTimestamps = new HashMap<String, Long>();

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
        Debug debug = new Debug(getClass());
        T object = objects.get(key);
        
        if (object != null)
            debug.print("Got cached entry for " + clazz.getSimpleName() + " with key " + key);

        return object;
    }

    public Collection<T> getAll() {
        return objects.values();
    }

    /**
     * Find an object using the given tester lambda.
     */
    public T find(CacheTester<T> tester) {
        Debug debug = new Debug(getClass());
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
        Debug debug = new Debug(getClass());

        if (objects.containsKey(object.getKey())) {
            debug.print("Skipping insertion for " + clazz.getSimpleName() + " " + object.getKey() + " - already exists.");
            return;
        }

        if (maxSize > 0) {
            while (objects.size() >= maxSize) {
                removeOldestEntry();
            }
        }

        if (maxMemoryUsage > 0) {
            Runnable punishmentMemoryReleaser = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        // punishExpiryRunnable.run();
                        int size = MemoryUtil.getSizeOf(object);

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
        }

        objects.put(object.getKey(), object);
        objectInsertionTimestamps.put(object.getKey(), System.currentTimeMillis());
        debug.print("Created cached entry for " + clazz.getSimpleName() + " with key " + object.getKey());
    }

    /**
     * Remove an object from the cache.
     */
    public T remove(T object) {
        Debug debug = new Debug(getClass());
        T removed = objects.remove(object);

        if (removed == null) {
            debug.print("Could not remove entry for " + clazz.getSimpleName() + " with key " + object.getKey() + " - does not exist");
            return null;
        }

        if (maxMemoryUsage > 0) {
            memoryUsage -= MemoryUtil.getSizeOf(object);
        }
        
        debug.print("Removed entry for " + clazz.getSimpleName() + " with key " + object.getKey());
        return removed;
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