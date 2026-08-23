package cz.martim12.noteindex.core.model;

import java.time.Instant;

/**
 * Represents a document persisted by NoteIndex.
 *
 * <p>A document contains both the original imported content and the searchable
 * representation used by the search subsystem, together with persistence
 * metadata assigned to the document.</p>
 *
 * @param id persistent identifier of the document
 * @param title display title of the document
 * @param sourceUri URI identifying the original source of the document
 * @param format format of the imported source
 * @param originalContent original content obtained from the source
 * @param searchableContent content prepared for indexing and searching
 * @param importedAt time at which the document was imported
 */
public record Document (
    long id,
    String title,
    String sourceUri,
    String format,
    String originalContent,
    String searchableContent,
    Instant importedAt

) {
}
