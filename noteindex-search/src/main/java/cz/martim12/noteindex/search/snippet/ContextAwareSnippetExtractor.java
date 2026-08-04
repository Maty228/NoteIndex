package cz.martim12.noteindex.search.snippet;

import cz.martim12.noteindex.search.analysis.AnalyzedToken;
import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryPhrase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Selects a context window containing the strongest concentration
 * of query matches.
 *
 * Exact phrase occurrences are preferred over standalone terms.
 */
public final class ContextAwareSnippetExtractor implements SnippetExtractor {

    private final TextAnalyzer analyzer;

    public ContextAwareSnippetExtractor(TextAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(
                analyzer,
                "Text analyzer must not be null"
        );
    }

    @Override
    public Snippet extract(CharSequence source, ParsedQuery query, int maximumLength) {
        Objects.requireNonNull(source, "Source text must not be null");
        Objects.requireNonNull(query, "Parsed query must not be null");

        if (maximumLength <= 0) {
            throw new IllegalArgumentException(
                    "Maximum snippet length must be positive"
            );
        }

        String text = source.toString();

        if (text.isEmpty()) {
            return new Snippet("", 0, 0, false, false);
        }

        List<AnalyzedToken> tokens = analyzer.analyze(text);
        List<MatchSpan> matches = findMatches(tokens, query);

        if (matches.isEmpty()) {
            return fallbackSnippet(text, maximumLength);
        }

        TextWindow bestWindow = null;
        WindowScore bestScore = null;

        for (MatchSpan anchor : matches) {
            TextWindow candidate = createWindow(text, anchor, maximumLength);

            WindowScore candidateScore = scoreWindow(candidate, matches);

            if (bestScore == null || candidateScore.isBetterThan(bestScore)) {
                bestWindow = candidate;
                bestScore = candidateScore;
            }
        }

        return toSnippet(text, bestWindow);
    }

    private static List<MatchSpan> findMatches(List<AnalyzedToken> tokens, ParsedQuery query) {
        List<MatchSpan> matches = new ArrayList<>();

        addPhraseMatches(tokens, query.requiredPhrases(), matches);
        addTermMatches(tokens, query.allTerms(), matches);

        return List.copyOf(matches);
    }

    private static void addPhraseMatches(List<AnalyzedToken> tokens, List<QueryPhrase> phrases, List<MatchSpan> destination) {
        for (QueryPhrase phrase : phrases) {
            int phraseLength = phrase.length();

            for (int start = 0; start <= tokens.size() - phraseLength; start++) {
                if (!matchesPhrase(tokens, start, phrase)) {
                    continue;
                }

                AnalyzedToken first = tokens.get(start);
                AnalyzedToken last = tokens.get(start + phraseLength - 1);

                destination.add(new MatchSpan(
                        first.startOffset(), last.endOffset(), null, true
                ));
            }
        }
    }

    private static boolean matchesPhrase(List<AnalyzedToken> tokens, int start, QueryPhrase phrase) {
        for (int offset = 0; offset < phrase.length(); offset++) {

            if (!tokens.get(start + offset).term().equals(phrase.terms().get(offset))) {
                return false;
            }
        }
        return true;
    }

    private static void addTermMatches(List<AnalyzedToken> tokens, List<String> queryTerms, List<MatchSpan> destination) {
        Set<String> terms = Set.copyOf(queryTerms);

        for (AnalyzedToken token : tokens) {
            if (!terms.contains(token.term())) {
                continue;
            }

            destination.add(new MatchSpan(
                    token.startOffset(), token.endOffset(), token.term(), false)
            );
        }
    }

    private static TextWindow createWindow(String text, MatchSpan anchor, int maximumLength) {
        if (text.length() <= maximumLength) {
            return new TextWindow(0, text.length());
        }

        int anchorLength = anchor.endOffset() - anchor.startOffset();

        int availableContext = Math.max(0, maximumLength - anchorLength);

        int start = Math.max(0, anchor.startOffset() - availableContext / 2);

        int end = Math.min(text.length(), start + maximumLength);

        if (end - start < maximumLength) {
            start = Math.max(0, end - maximumLength);
        }

        start = alignStartToWordBoundary(text, start, end);
        end = alignEndToWordBoundary(text, start, end);

        start = skipLeadingWhitespace(text, start, end);
        end = skipTrailingWhitespace(text, start, end);

        if (start >= end) {
            start = Math.max(0, anchor.startOffset());
            end = Math.min(text.length(), start + maximumLength);
        }

        return new TextWindow(start, end);
    }

    private static int alignStartToWordBoundary(String text, int start, int end) {
        while (start > 0 && start < end && isTokenCharacter(text.charAt(start - 1)) && isTokenCharacter(text.charAt(start))) {
            start++;
        }

        return start;
    }

    private static int alignEndToWordBoundary(String text, int start, int end) {
        while (end < text.length() && end > start && isTokenCharacter(text.charAt(end - 1)) && isTokenCharacter(text.charAt(end))) {
            end--;
        }

        return end;
    }

    private static boolean isTokenCharacter(char character) {
        return Character.isLetterOrDigit(character)
                || Character.getType(character)
                == Character.NON_SPACING_MARK
                || Character.getType(character)
                == Character.COMBINING_SPACING_MARK;
    }

    private static int skipLeadingWhitespace(String text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }

    private static int skipTrailingWhitespace(String text, int start, int end) {
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    private static WindowScore scoreWindow(TextWindow window, List<MatchSpan> matches) {
        int phraseOccurrences = 0;
        int totalTermOccurrences = 0;
        Set<String> distinctTerms = new HashSet<>();

        for (MatchSpan match : matches) {
            if (!window.contains(match)) {
                continue;
            }

            if (match.phrase()) {
                phraseOccurrences++;
            } else {
                totalTermOccurrences++;
                distinctTerms.add(match.term());
            }
        }

        return new WindowScore(phraseOccurrences, distinctTerms.size(), totalTermOccurrences, window.startOffset());
    }

    private static Snippet fallbackSnippet(String text, int maximumLength) {
        int end = Math.min(text.length(), maximumLength);

        end = alignEndToWordBoundary(text, 0, end);
        end = skipTrailingWhitespace(text, 0, end);

        if (end == 0) {
            end = Math.min(text.length(), maximumLength);
        }

        return new Snippet(text.substring(0, end), 0, end, false, end < text.length());
    }

    private static Snippet toSnippet(String text, TextWindow window) {
        return new Snippet(
                text.substring(window.startOffset(), window.endOffset()), window.startOffset(), window.endOffset(), window.startOffset() > 0, window.endOffset() < text.length()
        );
    }

    private record MatchSpan(
            int startOffset, int endOffset, String term, boolean phrase
    ) {}

    private record TextWindow(int startOffset, int endOffset) {
        private boolean contains(MatchSpan match) {
            return match.startOffset() >= startOffset && match.endOffset() <= endOffset;
        }
    }

    private record WindowScore(int phraseOccurrences, int distinctTermCount, int totalTermOccurrences, int startOffset) {
        private boolean isBetterThan(WindowScore other) {
            if (phraseOccurrences != other.phraseOccurrences) {
                return phraseOccurrences > other.phraseOccurrences;
            }

            if (distinctTermCount != other.distinctTermCount) {
                return distinctTermCount > other.distinctTermCount;
            }

            if (totalTermOccurrences != other.totalTermOccurrences) {
                return totalTermOccurrences > other.totalTermOccurrences;
            }

            return startOffset < other.startOffset;
        }
    }
}
