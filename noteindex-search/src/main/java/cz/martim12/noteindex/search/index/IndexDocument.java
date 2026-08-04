package cz.martim12.noteindex.search.index;

import java.util.*;

/**
 * Lightweight document representation supplied to the search index.
 *
 * It contains only the stable document ID and searchable field content.
 */
public record IndexDocument (long documentId, Map<FieldName, String> fields){
    public IndexDocument {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(fields, "Fields must not be null");

        if (fields.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one field must be provided"
            );
        }

        Map<FieldName, String> copiedFields = new LinkedHashMap<>();

        fields.forEach((name, content) -> {
            Objects.requireNonNull(name, "Field name must not be null");
            Objects.requireNonNull(content, "Field content must not be null");
            copiedFields.put(name, content);
        });

        fields = Collections.unmodifiableMap(copiedFields);

    }

    public Optional<String> field(FieldName name) {
        Objects.requireNonNull(name, "Field name must not be null");
        return Optional.ofNullable(fields.get(name));
    }
}
