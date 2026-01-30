package com.euphoria.party.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache<K, V> {
    
    private final Map<K, CacheEntry<V>> cache;
    private final long ttl;
    private final int maxSize;
    
    public Cache(long ttlMillis) {
        this(ttlMillis, 0); // 0 = unlimited
    }
    
    public Cache(long ttlMillis, int maxSize) {
        this.cache = new ConcurrentHashMap<>();
        this.ttl = ttlMillis;
        this.maxSize = maxSize;
    }
    
    public void put(K key, V value) {
        // Enforce max size if set
        if (maxSize > 0 && cache.size() >= maxSize) {
            // Remove oldest expired entry first, or random entry if none expired
            cleanExpired();
            if (cache.size() >= maxSize) {
                // Remove first entry (arbitrary, but maintains size limit)
                K firstKey = cache.keySet().iterator().next();
                cache.remove(firstKey);
            }
        }
        
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttl));
    }
    
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        return entry.value;
    }
    
    public void invalidate(K key) {
        cache.remove(key);
    }
    
    public void clear() {
        cache.clear();
    }
    
    public void cleanExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public int size() {
        return cache.size();
    }
    
    private static class CacheEntry<V> {
        private final V value;
        private final long expiryTime;
        
        CacheEntry(V value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
