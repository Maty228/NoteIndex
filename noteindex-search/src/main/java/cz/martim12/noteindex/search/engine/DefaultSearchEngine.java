package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.query.QueryPhrase;
import cz.martim12.noteindex.search.ranking.RankingStrategy;
import cz.martim12.noteindex.search.retrieval.CandidateRetriever;
import cz.martim12.noteindex.search.retrieval.PhraseMatch;
import cz.martim12.noteindex.search.retrieval.PhraseMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default orchestration of query parsing, candidate retrieval
 * and relevance ranking.
 */

public final class DefaultSearchEngine implements SearchEngine {

    private static final Comparator<SearchHit> HIT_ORDER =
            Comparator.comparingDouble(SearchHit::score)
                    .reversed()
                    .thenComparing(SearchHit::documentId);

    private final QueryParser queryParser;
    private final CandidateRetriever candidateRetriever;
    private final RankingStrategy rankingStrategy;
    private final PhraseMatcher phraseMatcher;
    private final List<FieldName> fields;
    private final double phraseOccurrenceBonus;

    public DefaultSearchEngine(
            QueryParser queryParser,
            CandidateRetriever candidateRetriever,
            RankingStrategy rankingStrategy,
            PhraseMatcher phraseMatcher,
            Collection<FieldName> fields,
            double phraseOccurrenceBonus
    ) {
        this.queryParser = Objects.requireNonNull(queryParser, "Query parser must not be null");

        this.candidateRetriever = Objects.requireNonNull(candidateRetriever, "Candidate retriever must not be null");

        this.rankingStrategy = Objects.requireNonNull(rankingStrategy, "Ranking strategy must not be null");

        this.phraseMatcher = Objects.requireNonNull(phraseMatcher, "Phrase matcher must not be null");

        this.fields = copyFields(fields);

        if (!Double.isFinite(phraseOccurrenceBonus) || phraseOccurrenceBonus < 0.0) {
            throw new IllegalArgumentException(
                    "Phrase occurrence bonus must be finite and non-negative"
            );
        }

        this.phraseOccurrenceBonus = phraseOccurrenceBonus;
    }

    @Override
    public List<SearchHit> search(CharSequence rawQuery, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Search result limit must be positive"
            );
        }

        ParsedQuery query = queryParser.parse(rawQuery);

        List<Long> candidateIds = candidateRetriever.retrieveCandidates(query);

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> phraseOccurrences = collectPhraseOccurrences(query);

        List<SearchHit> hits = new ArrayList<>(candidateIds.size());

        for (long documentId : candidateIds) {
            double lexicalScore = rankingStrategy.score(documentId, query);

            int occurrenceCount = phraseOccurrences.getOrDefault(documentId, 0);

            double phraseBoost = occurrenceCount * phraseOccurrenceBonus;

            hits.add(new SearchHit(documentId, lexicalScore, phraseBoost));
        }

        hits.sort(HIT_ORDER);

        if (hits.size() <= limit) {
            return List.copyOf(hits);
        }

        return List.copyOf(hits.subList(0, limit));
    }

    private Map<Long, Integer> collectPhraseOccurrences(ParsedQuery query) {
        if (!query.hasRequiredPhrases()) {
            return Map.of();
        }

        Map<Long, Integer> occurrencesByDocument = new HashMap<>();

        for (QueryPhrase phrase : query.requiredPhrases()) {
            for (PhraseMatch match : phraseMatcher.findMatches(phrase, fields)) {
                occurrencesByDocument.merge(match.documentId(), match.occurrenceCount(), Integer::sum);
            }
        }

        return Map.copyOf(occurrencesByDocument);
    }

    private static List<FieldName> copyFields(Collection<FieldName> fields) {
        Objects.requireNonNull(fields, "Search fields must not be null");

        LinkedHashSet<FieldName> uniqueFields = new LinkedHashSet<>();

        for (FieldName field : fields) {
            uniqueFields.add(Objects.requireNonNull(field, "Search field must not be null"));
        }

        if (uniqueFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one search field must be provided"
            );
        }

        return List.copyOf(uniqueFields);
    }
}
