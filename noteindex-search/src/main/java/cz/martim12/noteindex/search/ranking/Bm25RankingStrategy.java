package cz.martim12.noteindex.search.ranking;

import cz.martim12.noteindex.search.index.DocumentStatistics;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.FieldStatistics;
import cz.martim12.noteindex.search.index.IndexReader;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryPhrase;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;

import java.util.*;

/**
 * Field-aware BM25 ranking strategy.
 * Each configured field is scored independently and multiplied
 * by its assigned weight. Field scores are then added together.
 */
public final class Bm25RankingStrategy implements RankingStrategy {

    private final IndexReader indexReader;
    private final Bm25Parameters parameters;
    private final Map<FieldName, Double> fieldWeights;
    private final StandaloneTermMatchMode standaloneTermMatchMode;

    private static final double PREFIX_MATCH_WEIGHT = 0.85;


    public Bm25RankingStrategy(IndexReader indexReader, Map<FieldName, Double> fieldWeights) {
        this(
                indexReader, fieldWeights, Bm25Parameters.DEFAULT, StandaloneTermMatchMode.EXACT
        );
    }

    public Bm25RankingStrategy(
            IndexReader indexReader,
            Map<FieldName, Double> fieldWeights,
            StandaloneTermMatchMode standaloneTermMatchMode
    ) {
        this(
                indexReader,
                fieldWeights,
                Bm25Parameters.DEFAULT,
                standaloneTermMatchMode
        );
    }

    /**
     * Creates a BM25 ranking strategy.
     *
     * @param indexReader source of index statistics and postings
     * @param fieldWeights weights assigned to searchable fields
     * @param parameters BM25 tuning parameters
     * @param standaloneTermMatchMode matching mode for standalone terms
     */
    public Bm25RankingStrategy(
            IndexReader indexReader, Map<FieldName, Double> fieldWeights, Bm25Parameters parameters, StandaloneTermMatchMode standaloneTermMatchMode) {
        this.indexReader = Objects.requireNonNull(
                indexReader,
                "Index reader must not be null"
        );

        this.parameters = Objects.requireNonNull(
                parameters,
                "BM25 parameters must not be null"
        );

        this.fieldWeights = copyFieldWeights(fieldWeights);

        this.standaloneTermMatchMode = Objects.requireNonNull(
                standaloneTermMatchMode,
                "Standalone term match mode must not be null"
        );
    }

    public Bm25RankingStrategy(
            IndexReader indexReader,
            Map<FieldName, Double> fieldWeights,
            Bm25Parameters parameters
    ) {
        this(
                indexReader,
                fieldWeights,
                parameters,
                StandaloneTermMatchMode.EXACT
        );
    }

    /**
     * Calculates the BM25 relevance score for a document.
     *
     * @param documentId indexed document identifier
     * @param query parsed query
     * @return calculated relevance score
     */
    @Override
    public double score(long documentId, ParsedQuery query) {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(
                query,
                "Parsed query must not be null"
        );

        DocumentStatistics documentStatistics =
                indexReader.documentStatistics(documentId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Document is not indexed: "
                                                + documentId
                                )
                        );

        double score = 0.0;

        /*
         * Standalone terms follow the configured exact/prefix policy.
         */
        for (String term : query.terms()) {
            for (
                    Map.Entry<FieldName, Double> fieldEntry
                    : fieldWeights.entrySet()
            ) {
                score += scoreStandaloneTermInField(
                        documentId,
                        documentStatistics,
                        term,
                        fieldEntry.getKey(),
                        fieldEntry.getValue()
                );
            }
        }

        /*
         * Terms belonging only to required phrases stay exact.
         *
         * Avoid scoring a term twice when it also appeared as a
         * standalone query term.
         */
        for (String phraseTerm : phraseTerms(query)) {
            if (query.terms().contains(phraseTerm)) {
                continue;
            }

            for (Map.Entry<FieldName, Double> fieldEntry : fieldWeights.entrySet()) {
                score += scoreTermInField(
                        documentId,
                        documentStatistics,
                        phraseTerm,
                        fieldEntry.getKey(),
                        fieldEntry.getValue()
                );
            }
        }

        return score;
    }

    private double scoreStandaloneTermInField(
            long documentId,
            DocumentStatistics documentStatistics,
            String queryTerm,
            FieldName field,
            double fieldWeight
    ) {
        if (standaloneTermMatchMode
                == StandaloneTermMatchMode.EXACT) {

            return scoreTermInField(
                    documentId,
                    documentStatistics,
                    queryTerm,
                    field,
                    fieldWeight
            );
        }

        /*
         * Prefer an exact token whenever this document contains it.
         */
        double exactScore = scoreTermInField(
                documentId,
                documentStatistics,
                queryTerm,
                field,
                fieldWeight
        );

        if (exactScore > 0.0) {
            return exactScore;
        }

        double bestPrefixScore = 0.0;

        for (String indexedTerm :
                indexReader.termsWithPrefix(
                        queryTerm,
                        field
                )) {

            if (indexedTerm.equals(queryTerm)) {
                continue;
            }

            double prefixScore = scoreTermInField(
                    documentId,
                    documentStatistics,
                    indexedTerm,
                    field,
                    fieldWeight
            );

            bestPrefixScore = Math.max(
                    bestPrefixScore,
                    prefixScore
            );
        }

        return bestPrefixScore * PREFIX_MATCH_WEIGHT;
    }

    private static List<String> phraseTerms(
            ParsedQuery query
    ) {
        LinkedHashSet<String> terms =
                new LinkedHashSet<>();

        for (QueryPhrase phrase :
                query.requiredPhrases()) {

            terms.addAll(phrase.terms());
        }

        return List.copyOf(terms);
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
