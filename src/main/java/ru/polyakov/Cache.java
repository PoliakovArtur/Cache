package ru.polyakov;

import java.util.Map;

class Cache {

    private final String name;
    private final Map<MethodStateLock, Object> cache;

    Cache(String name, Map<MethodStateLock, Object> cache) {
        this.name = name;
        this.cache = cache;
    }

    void cacheValue(MethodStateLock state, Object value) {
        cache.put(state, value);
    }

    boolean contains(MethodStateLock state) {
        return cache.containsKey(state);
    }

    Object getOrNull(MethodStateLock state) {
        return cache.get(state);
    }

    void remove(MethodStateLock state) {
        cache.remove(state);
    }

    void clear() {
        cache.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cache c)) return false;
        return name.equals(c.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
