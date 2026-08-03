package cz.martim12.noteindex.search.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeTextAnalyzerTest {

    private final TextAnalyzer analyzer = new UnicodeTextAnalyzer();

    @Test
    void normalizesCaseAndSeparatesPunctuation() {
        List<AnalyzedToken> tokens = analyzer.analyze("Balanced BINARY-trees, work!");

        assertEquals(List.of("balanced", "binary", "trees", "work"), terms(tokens));
    }

    @Test
    void preservesCzechDiacritics() {
        List<AnalyzedToken> tokens = analyzer.analyze("Vyhledávání ve studijních poznámkách.");

        assertEquals(List.of("vyhledávání", "ve", "studijních", "poznámkách"), terms(tokens));
    }

    @Test
    void includesNumbersInTokens() {
        List<AnalyzedToken> tokens = analyzer.analyze("Java 25 and UTF8");

        assertEquals(List.of("java", "25", "and", "utf8"), terms(tokens));
    }

    @Test
    void recordsSequentialPositionsAndOriginalOffsets() {
        String text = "Java, SQLite!";

        List<AnalyzedToken> tokens = analyzer.analyze(text);

        assertEquals(new AnalyzedToken("java", 0, 0, 4), tokens.getFirst());

        assertEquals(new AnalyzedToken("sqlite", 1, 6, 12), tokens.getLast());

        assertEquals("Java", text.substring(
                        tokens.getFirst().startOffset(), tokens.getFirst().endOffset()));
    }

    @Test
    void normalizesEquivalentUnicodeForms() {
        String decomposed = "Cafe\u0301";

        List<AnalyzedToken> tokens = analyzer.analyze(decomposed);

        assertEquals("café", tokens.getFirst().term());


    }

    @Test
    void returnsEmptyListForTextWithoutTokens() {
        assertTrue(analyzer.analyze("... --- !!!").isEmpty());
    }




    private static List<String> terms(List<AnalyzedToken> tokens) {
        return tokens.stream().map(AnalyzedToken::term).toList();
    }

}

