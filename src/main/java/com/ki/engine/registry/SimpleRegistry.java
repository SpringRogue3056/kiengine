package com.ki.engine.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry with cached key/view collections.
 * Uses ConcurrentHashMap for lock-free reads and fine-grained locking on writes.
 */
public class SimpleRegistry<T> implements Registry<T> {
    private final Map<String, T> entries = new ConcurrentHashMap<>();
    private volatile Collection<T> cachedValues = Collections.emptyList();
    private volatile Collection<String> cachedKeys = Collections.emptyList();
    private volatile boolean cacheValid = false;

    @Override
    public T get(String id) {
        if (id == null) return null;
        return entries.get(id.toLowerCase(Locale.ROOT));
    }

    @Override
    public void register(String id, T value) {
        entries.put(id.toLowerCase(Locale.ROOT), value);
        cacheValid = false;
    }

    @Override
    public void unregister(String id) {
        entries.remove(id.toLowerCase(Locale.ROOT));
        cacheValid = false;
    }

    @Override
    public boolean has(String id) {
        if (id == null) return false;
        return entries.containsKey(id.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<T> values() {
        if (!cacheValid) rebuildCache();
        return cachedValues;
    }

    @Override
    public Collection<String> keys() {
        if (!cacheValid) rebuildCache();
        return cachedKeys;
    }

    @Override
    public void clear() {
        entries.clear();
        cacheValid = false;
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void registerAll(Map<String, T> newEntries) {
        for (Map.Entry<String, T> e : newEntries.entrySet()) {
            entries.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        cacheValid = false;
    }

    @Override
    public Map<String, T> getAll() {
        return new ConcurrentHashMap<>(entries);
    }

    private void rebuildCache() {
        cachedValues = Collections.unmodifiableCollection(new ArrayList<>(entries.values()));
        cachedKeys = Collections.unmodifiableCollection(new ArrayList<>(entries.keySet()));
        cacheValid = true;
    }
}
