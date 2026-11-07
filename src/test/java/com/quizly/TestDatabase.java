package com.quizly;

import com.quizly.db.Database;
import com.quizly.db.InMemoryDocumentStore;

import java.util.concurrent.ConcurrentHashMap;

public final class TestDatabase {
    private static final ConcurrentHashMap<String, InMemoryDocumentStore> STORES = new ConcurrentHashMap<>();
    private TestDatabase() {
    }

    public static Database create(String name) {
        String normalized = name.replaceAll("[^a-zA-Z0-9_-]", "-");
        return new Database(STORES.computeIfAbsent(normalized, ignored -> new InMemoryDocumentStore()));
    }
}
