package cz.martim12.noteindex.core.model;

public record ImportedDocument(
        String title,
        String sourceUri,
        String format,
        String originalContent,
        String searchableContent
) {
}
