package com.quizly.db;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public final class FirestoreDocumentStore implements DocumentStore {

    private static final String APP_NAME = "quizly";

    private final FirebaseApp app;
    private final Firestore firestore;
    private final boolean ownsApp;

    private FirestoreDocumentStore(FirebaseApp app, boolean ownsApp) {
        this.app = app;
        this.firestore = FirestoreClient.getFirestore(app);
        this.ownsApp = ownsApp;
    }

    public static FirestoreDocumentStore connect(String projectId) {
        try {
            FirebaseApp app;
            boolean ownsApp;
            List<FirebaseApp> apps = FirebaseApp.getApps();
            if (apps.stream().anyMatch(existing -> APP_NAME.equals(existing.getName()))) {
                app = FirebaseApp.getInstance(APP_NAME);
                ownsApp = false;
            } else {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(firebaseCredentials())
                        .setProjectId(projectId)
                        .build();
                app = FirebaseApp.initializeApp(options, APP_NAME);
                ownsApp = true;
            }
            return new FirestoreDocumentStore(app, ownsApp);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not connect to Firebase. Check QUIZLY_FIREBASE_PROJECT_ID and Google credentials.",
                    exception);
        }
    }

    private static GoogleCredentials firebaseCredentials() throws IOException {
        String credentialJson = System.getenv("QUIZLY_FIREBASE_CREDENTIALS_JSON");
        if (credentialJson != null && !credentialJson.isBlank()) {
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialJson.getBytes(StandardCharsets.UTF_8)));
        }
        return GoogleCredentials.getApplicationDefault();
    }

    @Override
    public Map<String, Object> find(String collection, String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(collection).document(id).get().get();
            return snapshot.exists() ? withId(snapshot) : null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading Firestore document", exception);
        } catch (ExecutionException exception) {
            throw failure("read Firestore document", exception);
        }
    }

    @Override
    public List<Map<String, Object>> findAll(String collection) {
        try {
            return firestore.collection(collection).get().get().getDocuments().stream()
                    .map(FirestoreDocumentStore::withId)
                    .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading Firestore collection", exception);
        } catch (ExecutionException exception) {
            throw failure("read Firestore collection", exception);
        }
    }

    @Override
    public void create(String collection, String id, Map<String, Object> document) {
        try {
            firestore.collection(collection).document(id).create(document).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating Firestore document", exception);
        } catch (ExecutionException exception) {
            throw failure("create Firestore document", exception);
        }
    }

    @Override
    public void set(String collection, String id, Map<String, Object> document) {
        await(firestore.collection(collection).document(id).set(document), "write Firestore document");
    }

    @Override
    public void update(String collection, String id, Map<String, Object> fields) {
        await(firestore.collection(collection).document(id).update(fields), "update Firestore document");
    }

    @Override
    public <T> T transact(Function<DocumentStore.Transaction, T> operation) {
        try {
            return firestore.runTransaction(transaction -> operation.apply(new DocumentStore.Transaction() {
                @Override
                public Map<String, Object> find(String collection, String id) {
                    try {
                        DocumentSnapshot snapshot = transaction.get(firestore.collection(collection).document(id)).get();
                        return snapshot.exists() ? snapshot.getData() : null;
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted during Firestore transaction read", exception);
                    } catch (ExecutionException exception) {
                        throw failure("read Firestore transaction document", exception);
                    }
                }

                @Override
                public void create(String collection, String id, Map<String, Object> document) {
                    transaction.create(firestore.collection(collection).document(id), document);
                }

                @Override
                public void set(String collection, String id, Map<String, Object> document) {
                    transaction.set(firestore.collection(collection).document(id), document);
                }
            })).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Firestore transaction", exception);
        } catch (ExecutionException exception) {
            throw failure("run Firestore transaction", exception);
        }
    }

    @Override
    public void close() {
        if (ownsApp) {
            try {
                firestore.close();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not close the Firestore client", exception);
            }
            try {
                app.delete();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not close the Firebase application", exception);
            }
        }
    }

    private void await(ApiFuture<?> future, String action) {
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to " + action, exception);
        } catch (ExecutionException exception) {
            throw failure(action, exception);
        }
    }

    private IllegalStateException failure(String action, Exception exception) {
        return new IllegalStateException("Could not " + action, exception.getCause() == null ? exception : exception.getCause());
    }

    private static Map<String, Object> withId(DocumentSnapshot snapshot) {
        Map<String, Object> data = new HashMap<>(snapshot.getData());
        data.put("_id", snapshot.getId());
        return data;
    }
}
