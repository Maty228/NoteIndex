package cz.martim12.noteindex.gui.importflow;

import cz.martim12.noteindex.core.model.Document;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
public record ImportBatchResult (
        List<Document> importedDocuments,
        List<Failure> failures
) {

    public ImportBatchResult {
        importedDocuments = List.copyOf(Objects.requireNonNull(importedDocuments, "Imported documents must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "Failures must not be null"));
    }

    public int totalProcessed() {
        return importedDocuments.size() + failures.size();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public record Failure(Path source, String message) {
        public Failure {
            source = Objects.requireNonNull(source, "Source must not be null");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Message must not be blank");
            }

            message = message.trim();
        }
    }
}
