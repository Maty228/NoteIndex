package cz.martim12.noteindex.search.query;

import cz.martim12.noteindex.search.analysis.AnalyzedToken;
import cz.martim12.noteindex.search.analysis.TextAnalyzer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Parses standalone terms and phrases enclosed in double quotes.
 * Examples:
 * java collections
 * "binary search tree"
 * java "virtual machine"
 */
public final class DefaultQueryParser implements QueryParser {

    private final TextAnalyzer analyzer;

    /**
     * Creates a query parser using the provided text analyzer.
     *
     * @param analyzer analyzer used to normalize query terms
     */
    public DefaultQueryParser(TextAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Text analyzer must not be null");
    }

    /**
     * Parses raw user input into a normalized query representation.
     *
     * <p>Supports standalone terms and required phrases enclosed in quotes.</p>
     *
     * @param rawQuery user-provided query text
     * @return parsed query
     * @throws QueryParseException if the query contains invalid syntax
     */
    @Override
    public ParsedQuery parse(CharSequence rawQuery) {
        Objects.requireNonNull(rawQuery, "Search query must not be null");

        String query = rawQuery.toString().trim();

        Set<String> standaloneTerms = new LinkedHashSet<>();
        Set<QueryPhrase> requiredPhrases = new LinkedHashSet<>();

        StringBuilder segment = new StringBuilder();
        boolean insidePhrase = false;

        for (int index = 0; index < query.length(); index++) {
            char current = query.charAt(index);

            if (current != '"') {
                segment.append(current);
                continue;
            }

            if (insidePhrase) {
                addPhrase(segment.toString(), requiredPhrases);
            } else {
                addStandaloneTerms(segment.toString(), standaloneTerms);
            }

            segment.setLength(0);
            insidePhrase = !insidePhrase;
        }

        if (insidePhrase) {
            throw new QueryParseException("Search query contains an unmatched quotation mark");
        }

        addStandaloneTerms(segment.toString(), standaloneTerms);

        if (standaloneTerms.isEmpty() && requiredPhrases.isEmpty()) {
            throw new QueryParseException("Search query does not contain any searchable terms");
        }

        return new ParsedQuery(
                List.copyOf(standaloneTerms),
                List.copyOf(requiredPhrases)
        );
    }

    private void addStandaloneTerms(String text, Set<String> destination) {
        analyzer.analyze(text)
                .stream()
                .map(AnalyzedToken::term)
                .forEach(destination::add);
    }

    private void addPhrase(String text, Set<QueryPhrase> destination) {

        List<String> terms = analyzer.analyze(text)
                .stream()
                .map(AnalyzedToken::term)
                .toList();

        if (terms.isEmpty()) {
            throw new QueryParseException("Quoted phrase does not contain searchable terms");
        }

        destination.add(new QueryPhrase(terms));
    }
}
