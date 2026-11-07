package com.quizly.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies every MongoDB collection into Firestore without deleting the source.
 */
public final class MongoToFirestoreMigrator {

    private MongoToFirestoreMigrator() {
    }

    public static void main(String[] args) {
        String mongoUri = setting("quizly.mongo.uri", "QUIZLY_MONGO_URI", "mongodb://localhost:27017");
        String mongoDatabaseName = setting("quizly.mongo.database", "QUIZLY_MONGO_DATABASE", "quizly");
        String projectId = setting("quizly.firebase.projectId", "QUIZLY_FIREBASE_PROJECT_ID", null);
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("Set QUIZLY_FIREBASE_PROJECT_ID before running the migration.");
        }

        try (MongoClient mongoClient = MongoClients.create(mongoUri);
             FirestoreDocumentStore firestore = FirestoreDocumentStore.connect(projectId)) {
            MongoDatabase source = mongoClient.getDatabase(mongoDatabaseName);
            List<String> collections = source.listCollectionNames().into(new ArrayList<>());
            long total = 0;
            for (String collectionName : collections) {
                if (collectionName.startsWith("system.")) {
                    continue;
                }
                long copied = copyCollection(source, collectionName, firestore);
                total += copied;
                System.out.printf("Copied %d document(s) from %s%n", copied, collectionName);
            }
            if (total == 0) {
                throw new IllegalStateException(
                        "The MongoDB source contained no documents; nothing was migrated.");
            }
            System.out.printf("Migration finished: %d document(s) copied. MongoDB data was not modified.%n", total);
        }
    }

    static long copyCollection(MongoDatabase source, String name, DocumentStore target) {
        long copied = 0;
        for (Document document : source.getCollection(name).find()) {
            Object rawId = document.get("_id");
            if (rawId == null) {
                throw new IllegalStateException("MongoDB document in " + name + " has no _id.");
            }
            String id = rawId instanceof ObjectId objectId ? objectId.toHexString() : rawId.toString();
            Map<String, Object> values = convertMap(document);
            values.remove("_id");
            target.set(name, id, values);
            if ("users".equals(name) && values.get("email") instanceof String email) {
                target.set("userEmailLookups", emailKey(email), Map.of("userId", id));
            }
            copied++;
        }
        return copied;
    }

    private static Map<String, Object> convertMap(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> {
            result.put(key, convert(value));
        });
        return result;
    }

    private static Object convert(Object value) {
        if (value instanceof Document document) {
            return convertMap(document);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(MongoToFirestoreMigrator::convert).toList();
        }
        if (value instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        if (value instanceof Decimal128 decimal) {
            BigDecimal number = decimal.bigDecimalValue();
            return number.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Binary binary) {
            return Base64.getEncoder().encodeToString(binary.getData());
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Date || value instanceof String || value instanceof Number || value instanceof Boolean
                || value == null) {
            return value;
        }
        return value.toString();
    }

    private static String emailKey(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value;
    }
}
