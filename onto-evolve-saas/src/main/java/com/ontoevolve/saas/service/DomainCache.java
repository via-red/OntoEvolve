package com.ontoevolve.saas.service;

import com.ontoevolve.saas.config.DomainDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DomainCache {

    private final ConcurrentMap<String, DomainDefinition> cache = new ConcurrentHashMap<>();

    public void put(String id, DomainDefinition def) {
        cache.put(id, def);
    }

    public DomainDefinition get(String id) {
        return cache.get(id);
    }

    public Map<String, DomainDefinition> getAll() {
        return Map.copyOf(cache);
    }

    public void remove(String id) {
        cache.remove(id);
    }

    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    public void clear() {
        cache.clear();
    }
}
