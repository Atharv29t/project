package com.quizly.db;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface DocumentStore extends AutoCloseable {

    Map<String, Object> find(String collection, String id);

    List<Map<String, Object>> findAll(String collection);

    void create(String collection, String id, Map<String, Object> document);

    void set(String collection, String id, Map<String, Object> document);

    void update(String collection, String id, Map<String, Object> fields);

    <T> T transact(Function<Transaction, T> operation);

    interface Transaction {
        Map<String, Object> find(String collection, String id);

        void create(String collection, String id, Map<String, Object> document);

        void set(String collection, String id, Map<String, Object> document);
    }

    @Override
    void close();
}
