package cz.martim12.noteindex.search.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParsedQueryTest {

    @Test
    void returnsDistinctTermsFromTermsAndPhrases() {
        ParsedQuery query = new ParsedQuery(
                List.of("java", "virtual"),
                List.of(
                        new QueryPhrase(
                                List.of(
                                        "java",
                                        "virtual",
                                        "machine"
                                )
                        )
                )
        );

        assertEquals(
                List.of("java", "virtual", "machine"),
                query.allTerms()
        );
    }

    @Test
    void createsDefensiveCopies() {
        List<String> terms =
                new ArrayList<>(List.of("java"));

        List<QueryPhrase> phrases =
                new ArrayList<>(
                        List.of(
                                new QueryPhrase(
                                        List.of(
                                                "virtual",
                                                "machine"
                                        )
                                )
                        )
                );

        ParsedQuery query =
                new ParsedQuery(terms, phrases);

        terms.add("sqlite");
        phrases.clear();

        assertEquals(List.of("java"), query.terms());
        assertEquals(1, query.requiredPhrases().size());
    }

    @Test
    void rejectsCompletelyEmptyQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ParsedQuery(
                        List.of(),
                        List.of()
                )
        );
    }
}