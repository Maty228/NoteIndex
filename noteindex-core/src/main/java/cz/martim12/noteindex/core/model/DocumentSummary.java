package cz.martim12.noteindex.core.model;

import java.time.Instant;
/**
 * Provides the lightweight document metadata used when full document content
 * is not required.
 *
 * @param id persistent identifier of the document
 * @param title display title of the document
 * @param format format of the imported source
 * @param importedAt time at which the document was imported
 */
public record DocumentSummary (
        long id,
        String title,
        String format,
        Instant importedAt
) {
}
