package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import cz.martim12.noteindex.search.query.DefaultQueryParser;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.ranking.RankingStrategy;
import cz.martim12.noteindex.search.retrieval.CandidateRetriever;
import cz.martim12.noteindex.search.retrieval.DefaultCandidateRetriever;
import cz.martim12.noteindex.search.retrieval.PhraseMatcher;
import cz.martim12.noteindex.search.retrieval.PositionalPhraseMatcher;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;
import cz.martim12.noteindex.search.ranking.Bm25RankingStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSearchEngineTest {

    private SearchIndex index;
    private QueryParser parser;
    private PhraseMatcher phraseMatcher;
    private CandidateRetriever candidateRetriever;

    @BeforeEach
    void setUp() {
        TextAnalyzer analyzer =
                new UnicodeTextAnalyzer();

        index = SearchIndexes.inMemory(analyzer);
        parser = new DefaultQueryParser(analyzer);
        phraseMatcher =
                new PositionalPhraseMatcher(index);

        candidateRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        )
                );
    }

    @Test
    void ordersHitsByDescendingScore() {
        index.indexDocument(document(1, "Java Basics", "Java virtual machine"));

        index.indexDocument(document(2, "Advanced Java", "Java reflection"));

        RankingStrategy ranking =
                (documentId, query) ->
                        documentId == 2 ? 5.0 : 2.0;

        SearchEngine engine = engine(ranking, 2.0);

        List<SearchHit> hits =
                engine.search("java", 10);

        assertEquals(
                List.of(2L, 1L),
                hits.stream()
                        .map(SearchHit::documentId)
                        .toList()
        );
    }

    @Test
    void breaksEqualScoresByDocumentId() {
        index.indexDocument(document(20, "Java", "Runtime"));

        index.indexDocument(document(5, "Java", "Runtime"));

        RankingStrategy equalRanking =
                (documentId, query) -> 1.0;

        SearchEngine engine =
                engine(equalRanking, 2.0);

        assertEquals(
                List.of(5L, 20L),
                engine.search("java", 10)
                        .stream()
                        .map(SearchHit::documentId)
                        .toList()
        );
    }

    @Test
    void limitsNumberOfReturnedHits() {
        index.indexDocument(document(1, "Java One", "Content"));

        index.indexDocument(document(2, "Java Two", "Content"));

        index.indexDocument(document(3, "Java Three", "Content"));

        RankingStrategy ranking =
                (documentId, query) ->
                        (double) documentId;

        SearchEngine engine = engine(ranking, 2.0);

        List<SearchHit> hits =
                engine.search("java", 2);

        assertEquals(2, hits.size());
        assertEquals(3, hits.getFirst().documentId());
        assertEquals(2, hits.getLast().documentId());
    }

    @Test
    void addsBoostForEveryPhraseOccurrence() {
        index.indexDocument(document(1, "Trees", "Binary tree"));

        index.indexDocument(document(2, "Trees", "Binary tree and another binary tree"));

        RankingStrategy equalRanking =
                (documentId, query) -> 1.0;

        SearchEngine engine =
                engine(equalRanking, 2.0);

        List<SearchHit> hits =
                engine.search("\"binary tree\"", 10);

        SearchHit first = hits.getFirst();
        SearchHit second = hits.getLast();

        assertEquals(2, first.documentId());
        assertEquals(4.0, first.phraseBoost());

        assertEquals(1, second.documentId());
        assertEquals(2.0, second.phraseBoost());
    }

    @Test
    void doesNotBoostStandaloneTerms() {
        index.indexDocument(document(1, "Binary Trees", "Binary tree content"));

        RankingStrategy ranking =
                (documentId, query) -> 3.0;

        SearchEngine engine = engine(ranking, 2.0);

        SearchHit hit =
                engine.search("binary tree", 10)
                        .getFirst();

        assertEquals(3.0, hit.lexicalScore());
        assertEquals(0.0, hit.phraseBoost());
        assertEquals(3.0, hit.score());
    }

    @Test
    void returnsEmptyListWhenNoCandidatesMatch() {
        index.indexDocument(document(1, "Java", "Virtual machine"));

        RankingStrategy ranking = (documentId, query) -> 1.0;

        SearchEngine engine = engine(ranking, 2.0);

        assertTrue(engine.search("sqlite", 10).isEmpty());
    }

    @Test
    void rejectsNonPositiveResultLimit() {
        SearchEngine engine = engine((documentId, query) -> 1.0, 2.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.search("java", 0)
        );
    }

    @Test
    void prefixSearchFindsPartialStandaloneTerm() {
        index.indexDocument(
                document(
                        1,
                        "Neural Networks",
                        "Deep learning"
                )
        );

        CandidateRetriever prefixRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(
                                FieldName.TITLE, 2.0,
                                FieldName.BODY, 1.0
                        ),
                        cz.martim12.noteindex.search.ranking.Bm25Parameters.DEFAULT,
                        StandaloneTermMatchMode.PREFIX
                );

        SearchEngine engine =
                new DefaultSearchEngine(
                        parser,
                        prefixRetriever,
                        ranking,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        2.0
                );

        List<SearchHit> hits =
                engine.search("neur", 10);

        assertEquals(1, hits.size());
        assertEquals(
                1,
                hits.getFirst().documentId()
        );

        assertTrue(
                hits.getFirst().lexicalScore() > 0.0
        );
    }

    @Test
    void exactSearchModeDoesNotFindPartialTerm() {
        index.indexDocument(
                document(
                        1,
                        "Neural Networks",
                        "Deep learning"
                )
        );

        CandidateRetriever exactRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.EXACT
                );

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(
                                FieldName.TITLE, 2.0,
                                FieldName.BODY, 1.0
                        ),
                        StandaloneTermMatchMode.EXACT
                );

        SearchEngine engine =
                new DefaultSearchEngine(
                        parser,
                        exactRetriever,
                        ranking,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        2.0
                );

        assertTrue(
                engine.search(
                        "neur",
                        10
                ).isEmpty()
        );
    }

    @Test
    void prefixModeDoesNotChangeQuotedPhraseMatching() {
        index.indexDocument(
                document(
                        1,
                        "Neural Networks",
                        "Virtual machines are useful"
                )
        );

        CandidateRetriever prefixRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(
                                FieldName.TITLE, 2.0,
                                FieldName.BODY, 1.0
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        SearchEngine engine =
                new DefaultSearchEngine(
                        parser,
                        prefixRetriever,
                        ranking,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        2.0
                );

        assertTrue(
                engine.search(
                        "\"virtual mach\"",
                        10
                ).isEmpty()
        );

        assertEquals(
                1,
                engine.search(
                        "\"virtual machines\"",
                        10
                ).size()
        );
    }

    @Test
    void prefixSearchFindsMultipleVocabularyExpansions() {
        index.indexDocument(
                document(
                        1,
                        "Neural Models",
                        "Learning"
                )
        );

        index.indexDocument(
                document(
                        2,
                        "Neuron Models",
                        "Biology"
                )
        );

        CandidateRetriever prefixRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(
                                FieldName.TITLE, 2.0,
                                FieldName.BODY, 1.0
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        SearchEngine engine =
                new DefaultSearchEngine(
                        parser,
                        prefixRetriever,
                        ranking,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        2.0
                );

        assertEquals(
                List.of(1L, 2L),
                engine.search(
                                "neur",
                                10
                        )
                        .stream()
                        .map(SearchHit::documentId)
                        .sorted()
                        .toList()
        );
    }

    private SearchEngine engine(RankingStrategy rankingStrategy, double phraseBonus) {
        return new DefaultSearchEngine(parser, candidateRetriever, rankingStrategy, phraseMatcher, List.of(FieldName.TITLE, FieldName.BODY), phraseBonus);
    }

    private static IndexDocument document(long id, String title, String body) {
        return new IndexDocument(id, Map.of(FieldName.TITLE, title, FieldName.BODY, body));
    }
}