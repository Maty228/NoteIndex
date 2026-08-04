package cz.martim12.noteindex.search.ranking;

import cz.martim12.noteindex.search.index.DocumentStatistics;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.FieldStatistics;
import cz.martim12.noteindex.search.index.IndexReader;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.query.ParsedQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/**
 * Field-aware BM25 ranking strategy.
 *
 * Each configured field is scored independently and multiplied
 * by its assigned weight. Field scores are then added together.
 */
public final class Bm25RankingStrategy implements RankingStrategy {

    private final IndexReader indexReader;
    private final Bm25Parameters parameters;
    private final Map<FieldName, Double> fieldWeights;

    public Bm25RankingStrategy(IndexReader indexReader, Map<FieldName, Double> fieldWeights) {
        this(
                indexReader, fieldWeights, Bm25Parameters.DEFAULT
        );
    }

    public Bm25RankingStrategy(IndexReader indexReader, Map<FieldName, Double> fieldWeights, Bm25Parameters parameters) {
        this.indexReader = Objects.requireNonNull(indexReader, "Index reader must not be null");
        this.parameters = Objects.requireNonNull(parameters, "BM25 parameters must not be null");
        this.fieldWeights = copyFieldWeights(fieldWeights);
    }

    @Override
    public double score(long documentId, ParsedQuery query) {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(query, "Parsed query must not be null");

        DocumentStatistics documentStatistics = indexReader.documentStatistics(documentId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Document is not indexed: " + documentId)
                );

        double score = 0.0;

        for (String term : query.allTerms()) {
            for (Map.Entry<FieldName, Double> fieldEntry : fieldWeights.entrySet()) {
                score += scoreTermInField(documentId, documentStatistics, term, fieldEntry.getKey(), fieldEntry.getValue());
            }
        }

        return score;
    }

    private double scoreTermInField(long documentId, DocumentStatistics documentStatistics, String term, FieldName field, double fieldWeight) {
        List<Posting> postings = indexReader.postings(term, field);

        if (postings.isEmpty()) {
            return 0.0;
        }

        Posting posting = findPosting(postings, documentId);

        if (posting == null) {
            return 0.0;
        }

        FieldStatistics fieldStatistics = indexReader.fieldStatistics(field);

        double averageFieldLength = fieldStatistics.averageFieldLength();

        if (fieldStatistics.documentsWithField() == 0 || averageFieldLength == 0.0) {
            return 0.0;
        }

        double inverseDocumentFrequency = inverseDocumentFrequency(fieldStatistics.documentsWithField(), postings.size());
        double normalizedTermFrequency = normalizedTermFrequency(posting.termFrequency(), documentStatistics.fieldLength(field), averageFieldLength);

        return fieldWeight
                * inverseDocumentFrequency
                * normalizedTermFrequency;

    }

    private double inverseDocumentFrequency(long documentCount, long documentFrequency) {
        return Math.log(1.0 + (documentCount - documentFrequency + 0.5) / (documentFrequency + 0.5));
    }

    private double normalizedTermFrequency(int termFrequency, int fieldLength, double averageFieldLength) {
        double lengthNormalization = 1.0 - parameters.b() + parameters.b() * fieldLength / averageFieldLength;

        double denominator = termFrequency + parameters.k1() * lengthNormalization;

        return termFrequency * (parameters.k1() + 1.0) / denominator;
    }

    /**
     * Posting lists are ordered by document ID, so binary search
     * avoids scanning the complete posting list for each score.
     */
    private static Posting findPosting(List<Posting> postings, long documentId) {
        int lower = 0;
        int upper = postings.size() - 1;

        while (lower <= upper) {
            int middle = (lower + upper) >>> 1;
            Posting posting = postings.get(middle);

            if (posting.documentId() == documentId) {
                return posting;
            }

            if (posting.documentId() < documentId) {
                lower = middle + 1;
            } else {
                upper = middle - 1;
            }
        }

        return null;
    }

    private static Map<FieldName, Double> copyFieldWeights(Map<FieldName, Double> fieldWeights) {
        Objects.requireNonNull(fieldWeights, "Field weights must not be null");

        if (fieldWeights.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one field weight must be provided"
            );
        }

        Map<FieldName, Double> copy = new LinkedHashMap<>();

        fieldWeights.forEach((field, weight) -> {
            Objects.requireNonNull(field, "Field name must not be null");
            Objects.requireNonNull(weight, "Field weight must not be null");
            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException(
                        "Field weight must be finite and positive"
                );
            }
            copy.put(field, weight);
        });


        return Collections.unmodifiableMap(copy);
    }
}
