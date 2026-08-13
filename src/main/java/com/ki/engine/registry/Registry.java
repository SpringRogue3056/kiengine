package com.ki.engine.registry;

import java.util.Collection;
import java.util.Map;

/**
 * Unified content registry interface - thread-safe, cached, extensible.
 * All content (items/blocks/entities/NPCs/recipes/skills) registers through this.
 */
public interface Registry<T> {
    T get(String id);
    void register(String id, T value);
    void unregister(String id);
    boolean has(String id);
    Collection<T> values();
    Collection<String> keys();
    void clear();
    int size();
    
    /** Batch operations for efficient bulk loading */
    void registerAll(Map<String, T> entries);
    Map<String, T> getAll();
}
