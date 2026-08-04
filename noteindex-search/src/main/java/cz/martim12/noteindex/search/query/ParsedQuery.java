package cz.martim12.noteindex.search.query;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Normalized representation of a user search query.
 *
 * @param terms standalone ranked terms
 * @param requiredPhrases exact phrases that results must contain
 */
public record ParsedQuery (
        List<String> terms,
        List<QueryPhrase> requiredPhrases
){
    public ParsedQuery {
        Objects.requireNonNull(terms, "Query terms must not be null");
        Objects.requireNonNull(requiredPhrases, "Required phrases must not be null");

        validateTerms(terms);

        for (QueryPhrase phrase : requiredPhrases) {
            Objects.requireNonNull(phrase, "Required phrase must not be null");
        }

        terms = List.copyOf(terms);
        requiredPhrases = List.copyOf(requiredPhrases);

        if (terms.isEmpty() && requiredPhrases.isEmpty()) {
            throw new IllegalArgumentException(
                    "Parsed query must not be empty"
            );
        }
    }

    /**
     * Returns every distinct normalized term used by the query.
     */
    public List<String> allTerms() {
        Set<String> allTerms = new LinkedHashSet<>(terms);

        for (QueryPhrase phrase : requiredPhrases) {
            allTerms.addAll(phrase.terms());
        }

        return List.copyOf(allTerms);
    }

    public boolean hasRequiredPhrases() {
        return !requiredPhrases.isEmpty();
    }

    private static void validateTerms(List<String> terms) {
        for (String term : terms) {
            Objects.requireNonNull(term, "Query term must not be null");

            if (term.isBlank()) {
                throw new IllegalArgumentException(
                        "Query term must not be blank"
                );
            }
        }
    }
}
