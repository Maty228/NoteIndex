package cz.martim12.noteindex.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents one ranked result returned by a search operation.
 *
 * <p>The result contains lightweight document metadata, its relevance score,
 * a presentation snippet, and highlight ranges for both the snippet and the
 * complete document content. Highlight lists are defensively copied when the
 * result is created.</p>
 *
 * @param document metadata identifying the matched document
 * @param score relevance score assigned to the result
 * @param snippet text excerpt presented for the search result
 * @param snippetHighlights ranges to highlight within {@code snippet}
 * @param contentHighlights ranges to highlight within the complete document content
 */
public record SearchResult(DocumentSummary document, double score, String snippet, List<HighlightRange> snippetHighlights, List<HighlightRange> contentHighlights) {

    /**
     * Validates the result data and creates defensive copies of the highlight lists.
     *
     * @param document metadata identifying the matched document
     * @param score relevance score assigned to the result
     * @param snippet text excerpt presented for the search result
     * @param snippetHighlights ranges to highlight within {@code snippet}
     * @param contentHighlights ranges to highlight within the complete document content
     * @throws NullPointerException if {@code document}, {@code snippet},
     *                              {@code snippetHighlights}, or
     *                              {@code contentHighlights} is {@code null},
     *                              or if either highlight list contains a
     *                              {@code null} element
     */
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
     * Creates a search result without highlight ranges.
     *
     * <p>Both highlight lists are initialized as empty lists.</p>
     *
     * @param document metadata identifying the matched document
     * @param score relevance score assigned to the result
     * @param snippet text excerpt presented for the search result
     * @throws NullPointerException if {@code document} or {@code snippet} is
     *                              {@code null}
     */
    public SearchResult(DocumentSummary document, double score, String snippet) {
        this(document, score, snippet, List.of(), List.of());
    }
}
