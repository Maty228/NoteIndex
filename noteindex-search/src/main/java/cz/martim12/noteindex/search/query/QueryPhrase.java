package cz.martim12.noteindex.search.query;


import java.util.List;
import java.util.Objects;

/**
 * A required sequence of normalized terms.
 */
public record QueryPhrase (
        List<String> terms
) {

    /**
     * Creates a validated query phrase.
     *
     * @param terms normalized phrase terms
     * @throws IllegalArgumentException if the phrase contains no terms
     */
    public QueryPhrase {
        Objects.requireNonNull(terms, "Phrase terms must not be null");

        if (terms.isEmpty()) {
            throw new IllegalArgumentException(
                    "Query phrase must contain at least one term"
            );
        }

        for (String term : terms) {
            Objects.requireNonNull(term, "Phrase term must not be null");

            if (term.isBlank()) {
                throw new IllegalArgumentException(
                        "Phrase term must not be blank"
                );
            }
        }

        terms = List.copyOf(terms);
    }

    /**
     * Returns the number of terms in the phrase.
     *
     * @return phrase length
     */
    public int length() {
        return terms.size();
    }
}
