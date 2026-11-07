package com.quizly.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoToFirestoreMigratorTest {

    @Test
    void copiesMongoDocumentsAndBuildsFirestoreEmailLookups() {
        MongoServer server = new MongoServer(new MemoryBackend());
        InetSocketAddress address = server.bind();
        try (MongoClient client = MongoClients.create("mongodb://127.0.0.1:" + address.getPort())) {
            var source = client.getDatabase("quizly");
            Date createdAt = new Date();
            source.getCollection("users").insertOne(new Document("_id", "user-1")
                    .append("name", "Ada")
                    .append("email", "ada@example.com")
                    .append("createdAt", createdAt));
            source.getCollection("results").insertOne(new Document("_id", "result-1")
                    .append("userId", "user-1")
                    .append("reviews", List.of(new Document("prompt", "Question"))));

            var target = new Database(new InMemoryDocumentStore());
            assertEquals(1, MongoToFirestoreMigrator.copyCollection(source, "users", target.store()));
            assertEquals(1, MongoToFirestoreMigrator.copyCollection(source, "results", target.store()));
            assertEquals(1, MongoToFirestoreMigrator.copyCollection(source, "users", target.store()));

            assertEquals("Ada", target.store().find("users", "user-1").get("name"));
            assertEquals(createdAt, target.store().find("users", "user-1").get("createdAt"));
            assertEquals("user-1", target.store().findAll("userEmailLookups").get(0).get("userId"));
            assertEquals(1, target.store().findAll("results").size());
        } finally {
            server.shutdown();
        }
    }
}
