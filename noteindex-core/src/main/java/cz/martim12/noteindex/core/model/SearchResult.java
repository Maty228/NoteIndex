package cz.martim12.noteindex.core.model;

import java.util.List;
import java.util.Objects;

public record SearchResult(DocumentSummary document, double score, String snippet, List<HighlightRange> snippetHighlights, List<HighlightRange> contentHighlights) {

    public SearchResult {
        Objects.requireNonNull(
                document,
                "Document must not be null"
        );

        Objects.requireNonNull(
                snippet,
                "Snippet must not be null"
        );

        Objects.requireNonNull(
                snippetHighlights,
                "Snippet highlights must not be null"
        );

        Objects.requireNonNull(
                contentHighlights,
                "Content highlights must not be null"
        );

        snippetHighlights =
                List.copyOf(snippetHighlights);

        contentHighlights =
                List.copyOf(contentHighlights);
    }

    /**
     * Compatibility constructor for non-highlighted results.
     */
    public SearchResult(DocumentSummary document, double score, String snippet) {
        this(document, score, snippet, List.of(), List.of());
    }
}