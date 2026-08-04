package cz.martim12.noteindex.search.query;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultQueryParserTest {

    private final QueryParser parser =
            new DefaultQueryParser(
                    new UnicodeTextAnalyzer()
            );

    @Test
    void parsesAndNormalizesStandaloneTerms() {
        ParsedQuery query =
                parser.parse("Java, SQLITE java");

        assertEquals(
                List.of("java", "sqlite"),
                query.terms()
        );

        assertTrue(query.requiredPhrases().isEmpty());
    }

    @Test
    void parsesQuotedPhrase() {
        ParsedQuery query =
                parser.parse("\"Binary search tree\"");

        assertTrue(query.terms().isEmpty());

        assertEquals(
                List.of(
                        new QueryPhrase(
                                List.of(
                                        "binary",
                                        "search",
                                        "tree"
                                )
                        )
                ),
                query.requiredPhrases()
        );
    }

    @Test
    void parsesMixedQuery() {
        ParsedQuery query =
                parser.parse(
                        "Java \"virtual machine\" garbage collection"
                );

        assertEquals(
                List.of(
                        "java",
                        "garbage",
                        "collection"
                ),
                query.terms()
        );

        assertEquals(
                List.of(
                        new QueryPhrase(
                                List.of("virtual", "machine")
                        )
                ),
                query.requiredPhrases()
        );
    }

    @Test
    void analyzesPunctuationInsidePhrase() {
        ParsedQuery query =
                parser.parse("\"binary-tree\"");

        assertEquals(
                List.of(
                        new QueryPhrase(
                                List.of("binary", "tree")
                        )
                ),
                query.requiredPhrases()
        );
    }

    @Test
    void preservesCzechDiacritics() {
        ParsedQuery query =
                parser.parse("Vyhledávání \"studijní poznámky\"");

        assertEquals(
                List.of("vyhledávání"),
                query.terms()
        );

        assertEquals(
                List.of(
                        new QueryPhrase(
                                List.of(
                                        "studijní",
                                        "poznámky"
                                )
                        )
                ),
                query.requiredPhrases()
        );
    }

    @Test
    void removesDuplicateTermsAndPhrases() {
        ParsedQuery query =
                parser.parse(
                        "java JAVA \"virtual machine\" "
                                + "\"virtual machine\""
                );

        assertEquals(List.of("java"), query.terms());
        assertEquals(1, query.requiredPhrases().size());
    }

    @Test
    void rejectsUnmatchedQuotationMark() {
        assertThrows(
                QueryParseException.class,
                () -> parser.parse(
                        "java \"virtual machine"
                )
        );
    }

    @Test
    void rejectsEmptyQuotedPhrase() {
        assertThrows(
                QueryParseException.class,
                () -> parser.parse("java \"   \"")
        );
    }

    @Test
    void rejectsQueryWithoutSearchableTerms() {
        assertThrows(
                QueryParseException.class,
                () -> parser.parse("... --- !!!")
        );
    }
}