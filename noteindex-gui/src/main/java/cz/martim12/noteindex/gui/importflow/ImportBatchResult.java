package cz.martim12.noteindex.gui.importflow;

import cz.martim12.noteindex.core.model.Document;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a batch document import operation.
 *
 * @param importedDocuments successfully imported documents
 * @param failures failed import attempts
 */
public record ImportBatchResult (
        List<Document> importedDocuments,
        List<Failure> failures
) {

    /**
     * Creates a validated import batch result.
     *
     * @param importedDocuments successfully imported documents
     * @param failures failed import attempts
     */
    public ImportBatchResult {
        importedDocuments = List.copyOf(Objects.requireNonNull(importedDocuments, "Imported documents must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "Failures must not be null"));
    }

    /**
     * Returns the total number of processed files.
     *
     * @return imported files plus failures
     */
    public int totalProcessed() {
        return importedDocuments.size() + failures.size();
    }

    /**
     * Checks whether any imports failed.
     *
     * @return true if failures exist
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    /**
     * Represents one failed import attempt.
     *
     * @param source failed source file
     * @param message failure description
     */
    public record Failure(Path source, String message) {

        /**
         * Creates a validated import failure.
         *
         * @param source failed source file
         * @param message failure description
         * @throws NullPointerException if the source path is null
         * @throws IllegalArgumentException if the message is blank
         */
        public Failure {
            Objects.requireNonNull(source, "Source must not be null");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Message must not be blank");
            }

            message = message.trim();
        }
    }
}
