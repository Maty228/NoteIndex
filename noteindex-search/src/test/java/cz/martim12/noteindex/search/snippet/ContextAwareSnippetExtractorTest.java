package cz.martim12.noteindex.search.snippet;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.query.DefaultQueryParser;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextAwareSnippetExtractorTest {

    private QueryParser parser;
    private SnippetExtractor extractor;

    @BeforeEach
    void setUp() {
        UnicodeTextAnalyzer analyzer =
                new UnicodeTextAnalyzer();

        parser = new DefaultQueryParser(analyzer);
        extractor =
                new ContextAwareSnippetExtractor(analyzer);
    }

    @Test
    void prefersExactPhraseOverStandaloneTerm() {
        String text =
                "Java is introduced at the beginning. "
                        + "This section contains unrelated filler. "
                        + "A binary search tree stores ordered values.";

        ParsedQuery query =
                parser.parse("java \"binary search tree\"");

        Snippet snippet =
                extractor.extract(text, query, 55);

        assertTrue(
                snippet.text().contains(
                        "binary search tree"
                )
        );

        assertFalse(snippet.text().startsWith("Java"));
        assertTrue(snippet.truncatedAtStart());
    }

    @Test
    void selectsDensestClusterOfQueryTerms() {
        String text =
                "Java appears once near the beginning. "
                        + "There is a large unrelated section here. "
                        + "Java collections use lists, sets and maps.";

        ParsedQuery query =
                parser.parse("java collections lists maps");

        Snippet snippet =
                extractor.extract(text, query, 60);

        assertTrue(snippet.text().contains("Java collections"));
        assertTrue(snippet.text().contains("lists"));
        assertTrue(snippet.text().contains("maps"));
    }

    @Test
    void preservesOriginalCaseAndDiacritics() {
        String text =
                "Úvod. VYHLEDÁVÁNÍ ve studijních poznámkách.";

        ParsedQuery query =
                parser.parse("vyhledávání poznámkách");

        Snippet snippet =
                extractor.extract(text, query, 100);

        assertTrue(snippet.text().contains("VYHLEDÁVÁNÍ"));
        assertTrue(snippet.text().contains("poznámkách"));
    }

    @Test
    void returnsWholeTextWhenItFits() {
        String text = "Java virtual machine";

        Snippet snippet = extractor.extract(
                text,
                parser.parse("java"),
                100
        );

        assertEquals(text, snippet.text());
        assertFalse(snippet.truncatedAtStart());
        assertFalse(snippet.truncatedAtEnd());
    }

    @Test
    void addsCorrectTruncationInformation() {
        String text =
                "Beginning context before the important "
                        + "binary tree section and trailing context.";

        Snippet snippet = extractor.extract(
                text,
                parser.parse("\"binary tree\""),
                35
        );

        assertTrue(snippet.truncatedAtStart());
        assertTrue(snippet.truncatedAtEnd());
        assertTrue(snippet.displayText().startsWith("..."));
        assertTrue(snippet.displayText().endsWith("..."));
    }

    @Test
    void fallsBackToBeginningWhenNoTermsArePresent() {
        String text =
                "This document does not contain the requested word.";

        Snippet snippet = extractor.extract(
                text,
                parser.parse("sqlite"),
                20
        );

        assertEquals(0, snippet.sourceStartOffset());
        assertTrue(snippet.truncatedAtEnd());
    }

    @Test
    void returnsEmptySnippetForEmptyText() {
        Snippet snippet = extractor.extract(
                "",
                parser.parse("java"),
                50
        );

        assertEquals("", snippet.text());
        assertFalse(snippet.truncatedAtStart());
        assertFalse(snippet.truncatedAtEnd());
    }

    @Test
    void rejectsNonPositiveMaximumLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extract(
                        "Java",
                        parser.parse("java"),
                        0
                )
        );
    }

    @Test
    void keepsExactPhraseIntactWhenItExceedsPreferredLength() {
        String text =
                "Prefix before an exceptionally long exact phrase "
                        + "and trailing content.";

        ParsedQuery query =
                parser.parse(
                        "\"exceptionally long exact phrase\""
                );

        Snippet snippet =
                extractor.extract(text, query, 12);

        assertTrue(
                snippet.text().contains(
                        "exceptionally long exact phrase"
                )
        );

        assertTrue(snippet.text().length() > 12);
    }
}