package cz.martim12.noteindex.core.model;

/**
 * Represents the textual input of a search request.
 *
 * @param text query text to be interpreted by the search subsystem
 */
public record SearchQuery (
        String text
) {
}
