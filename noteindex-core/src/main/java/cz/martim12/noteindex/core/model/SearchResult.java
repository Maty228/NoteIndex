package cz.martim12.noteindex.core.model;

public record SearchResult(
        DocumentSummary document,
        double score,
        String snippet
) {
}
