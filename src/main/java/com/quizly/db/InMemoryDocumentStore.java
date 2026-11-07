package com.quizly.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class InMemoryDocumentStore implements DocumentStore {

    private final Map<String, Map<String, Map<String, Object>>> collections = new ConcurrentHashMap<>();

    @Override
    public synchronized Map<String, Object> find(String collection, String id) {
        Map<String, Object> document = collection(collection).get(id);
        if (document == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>(document);
        copy.put("_id", id);
        return copy;
    }

    @Override
    public synchronized List<Map<String, Object>> findAll(String collection) {
        return collection(collection).entrySet().stream().map(entry -> {
            Map<String, Object> copy = new HashMap<>(entry.getValue());
            copy.put("_id", entry.getKey());
            return copy;
        }).toList();
    }

    @Override
    public synchronized void create(String collection, String id, Map<String, Object> document) {
        if (collection(collection).containsKey(id)) {
            throw new IllegalStateException("Document already exists: " + id);
        }
        collection(collection).put(id, new HashMap<>(document));
    }

    @Override
    public synchronized void set(String collection, String id, Map<String, Object> document) {
        collection(collection).put(id, new HashMap<>(document));
    }

    @Override
    public synchronized void update(String collection, String id, Map<String, Object> fields) {
        Map<String, Object> document = collection(collection).get(id);
        if (document == null) {
            throw new IllegalStateException("Document does not exist: " + id);
        }
        document.putAll(fields);
    }

    @Override
    public synchronized <T> T transact(Function<Transaction, T> operation) {
        return operation.apply(new Transaction() {
            @Override
            public Map<String, Object> find(String collection, String id) {
                return InMemoryDocumentStore.this.find(collection, id);
            }

            @Override
            public void create(String collection, String id, Map<String, Object> document) {
                InMemoryDocumentStore.this.create(collection, id, document);
            }

            @Override
            public void set(String collection, String id, Map<String, Object> document) {
                InMemoryDocumentStore.this.set(collection, id, document);
            }
        });
    }

    private Map<String, Map<String, Object>> collection(String name) {
        return collections.computeIfAbsent(name, ignored -> new ConcurrentHashMap<>());
    }

    @Override
    public void close() {
        collections.clear();
    }
}
