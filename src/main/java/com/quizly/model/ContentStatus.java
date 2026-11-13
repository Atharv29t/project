package com.quizly.model;

import java.util.Locale;

/** Publication state shared by quiz tracks and their questions. */
public enum ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    public static ContentStatus from(Object value) {
        if (value == null) {
            return PUBLISHED;
        }
        try {
            return ContentStatus.valueOf(value.toString().strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DRAFT;
        }
    }

    public String firestoreValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
