package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.ranking.Bm25Parameters;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration used when assembling the default search engine.
 *
 * @param fields fields searched by retrieval and phrase matching
 * @param fieldWeights BM25 weight assigned to every search field
 * @param bm25Parameters BM25 tuning parameters
 * @param phraseOccurrenceBonus score added for each phrase occurrence
 */
public record SearchConfiguration (
        List<FieldName> fields,
        Map<FieldName, Double> fieldWeights,
        Bm25Parameters bm25Parameters,
        double phraseOccurrenceBonus
) {

    /**
     * Creates the default search configuration.
     *
     * @return default search configuration
     */
    public static SearchConfiguration defaults() {
        return new SearchConfiguration(
                List.of(FieldName.TITLE, FieldName.BODY),
                Map.of(FieldName.TITLE, 3.0, FieldName.BODY, 1.0),
                Bm25Parameters.DEFAULT,
                2.0
        );
    }


    /**
     * Creates a validated search configuration.
     *
     * @param fields fields searched by retrieval and phrase matching
     * @param fieldWeights BM25 weight assigned to every search field
     * @param bm25Parameters BM25 tuning parameters
     * @param phraseOccurrenceBonus score added for each phrase occurrence
     * @throws NullPointerException if required values are null
     * @throws IllegalArgumentException if fields, weights, or scoring parameters are invalid
     */
    public SearchConfiguration {
        Objects.requireNonNull(
                fields,
                "Search fields must not be null"
        );

        Set<FieldName> uniqueFields =
                new LinkedHashSet<>();

        for (FieldName field : fields) {
            FieldName nonNullField = Objects.requireNonNull(field, "Search field must not be null");
            if (!uniqueFields.add(nonNullField)) {
                throw new IllegalArgumentException(
                        "Duplicate search field: " + nonNullField.value()
                );
            }
        }

        if (uniqueFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one search field must be provided"
            );
        }

        fields = List.copyOf(uniqueFields);

        Objects.requireNonNull(fieldWeights, "Field weights must not be null");

        if (fieldWeights.size() != fields.size() || !fieldWeights.keySet().containsAll(fields)) {
            throw new IllegalArgumentException(
                    "Every search field must have exactly one weight"
            );
        }

        Map<FieldName, Double> copiedWeights = new LinkedHashMap<>();

        /*
         * Copy weights in field order so floating-point score
         * accumulation remains deterministic.
         */
        for (FieldName field : fields) {
            Double weight = Objects.requireNonNull(fieldWeights.get(field), "Field weight must not be null");

            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException(
                        "Field weight must be finite and positive"
                );
            }

            copiedWeights.put(field, weight);
        }

        fieldWeights = Collections.unmodifiableMap(copiedWeights);

        Objects.requireNonNull(bm25Parameters, "BM25 parameters must not be null");

        if (!Double.isFinite(phraseOccurrenceBonus) || phraseOccurrenceBonus < 0.0) {
            throw new IllegalArgumentException(
                    "Phrase occurrence bonus must be finite and non-negative"
            );
        }
    }
}
