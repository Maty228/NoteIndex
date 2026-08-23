package cz.martim12.noteindex.search.analysis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic Unicode-aware analyzer used for both documents and queries.
 * Letters, combining marks and numbers form tokens.
 * Punctuation and whitespace act as separators.
 */
public final class UnicodeTextAnalyzer implements TextAnalyzer {
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    /**
     * Analyzes text into normalized Unicode-aware tokens.
     *
     * @param text text to analyze
     * @return immutable list of analyzed tokens ordered by position
     * @throws NullPointerException if the text is null
     */
    @Override
    public List<AnalyzedToken> analyze(CharSequence text) {
        Objects.requireNonNull(text, "Text must not be null");

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        List<AnalyzedToken> tokens = new ArrayList<>();

        int position = 0;

        while (matcher.find()) {
            String normalizedTerm = normalize(matcher.group());

            tokens.add(new AnalyzedToken(
                    normalizedTerm,
                    position,
                    matcher.start(),
                    matcher.end()
            ));
            position++;
        }

        return List.copyOf(tokens);
    }

    private static String normalize(String token) {
        return Normalizer.normalize(token, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
