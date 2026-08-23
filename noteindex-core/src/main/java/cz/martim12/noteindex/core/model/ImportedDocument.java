package cz.martim12.noteindex.core.model;

/**
 * Represents document data produced by an importer before it is persisted.
 *
 * <p>Unlike {@link Document}, an imported document does not yet contain a
 * persistent identifier or import timestamp.</p>
 *
 * @param title display title of the imported document
 * @param sourceUri URI identifying the original source
 * @param format format of the imported source
 * @param originalContent original content obtained from the source
 * @param searchableContent content prepared for indexing and searching
 */
public record ImportedDocument (
        String title,
        String sourceUri,
        String format,
        String originalContent,
        String searchableContent
) {
}
