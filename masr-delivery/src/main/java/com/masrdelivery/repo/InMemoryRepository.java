package com.masrdelivery.repo;

import com.masrdelivery.exception.DuplicateEntityException;
import com.masrdelivery.exception.EntityNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


public final class InMemoryRepository<T> {

    private final String typeName;
    private final Function<T, String> keyOf;
    private final Map<String, T> byKey = new ConcurrentHashMap<>();

    public InMemoryRepository(String typeName, Function<T, String> keyOf) {
        this.typeName = typeName;
        this.keyOf = keyOf;
    }

    public T add(T entity) throws DuplicateEntityException {
        String key = keyOf.apply(entity);
        if (byKey.putIfAbsent(key, entity) != null)
            throw new DuplicateEntityException(typeName + " with id " + key + " already exists.");
        return entity;
    }

    public Optional<T> find(String key) { return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key.trim())); }

    public T get(String key) throws EntityNotFoundException {
        return find(key).orElseThrow(() -> new EntityNotFoundException("No " + typeName.toLowerCase() + " with id " + key + "."));
    }

    public T remove(String key) throws EntityNotFoundException {
        T removed = key == null ? null : byKey.remove(key.trim());
        if (removed == null) throw new EntityNotFoundException("No " + typeName.toLowerCase() + " with id " + key + ".");
        return removed;
    }

    public List<T> all() { return List.copyOf(byKey.values()); }
    public int size() { return byKey.size(); }
    public boolean contains(String key) { return find(key).isPresent(); }
}
