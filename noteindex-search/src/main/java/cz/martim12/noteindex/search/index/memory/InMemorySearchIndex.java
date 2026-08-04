package cz.martim12.noteindex.search.index.memory;

import cz.martim12.noteindex.search.analysis.AnalyzedToken;
import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.index.DocumentStatistics;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.FieldStatistics;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.index.SearchIndex;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.*;

/**
 * Positional inverted index stored entirely in application memory.

 * Full document text is not retained. The index stores only terms,
 * document IDs, token positions and collection statistics.
 */
public final class InMemorySearchIndex implements SearchIndex{

    private final TextAnalyzer analyzer;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    /*
     * Field
     *   -> normalized term
     *      -> document ID
     *         -> posting
     */
    private final Map<FieldName, Map<String, NavigableMap<Long, Posting>>> postingsByField = new HashMap<>();

    private final Map<Long, DocumentStatistics> statisticsByDocument = new HashMap<>();

    /*
     * Stores which terms belong to each document field.
     * This will allow documents to be removed or replaced.
     */
    private final Map<Long, Map<FieldName, Set<String>>> termsByDocument = new HashMap<>();

    private final Map<FieldName, MutableFieldStatistics> statisticsByField = new HashMap<>();

    public InMemorySearchIndex(TextAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Analyzer must not be null");
    }

    @Override
    public void indexDocument(IndexDocument document) {
        Objects.requireNonNull(document, "Index document must not be null");

        writeLock.lock();

        try {
            PreparedDocument preparedDocument = prepare(document);

            if (statisticsByDocument.containsKey(document.documentId())) {
                removeDocument(document.documentId());
            }
            addPreparedDocument(preparedDocument);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<Posting> postings(String normalizedTerm, FieldName field) {
        requireTerm(normalizedTerm);
        Objects.requireNonNull(field, "Field name must not be null");

        readLock.lock();

        try {
            Map<String, NavigableMap<Long, Posting>> fieldPostings = postingsByField.get(field);

            if (fieldPostings == null) {
                return List.of();
            }

            NavigableMap<Long, Posting> termPostings = fieldPostings.get(normalizedTerm);

            if (termPostings == null) {
                return List.of();
            }
            return List.copyOf(termPostings.values());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Optional<DocumentStatistics> documentStatistics(long documentId) {
        requirePositiveDocumentId(documentId);

        readLock.lock();

        try {
            return Optional.ofNullable(statisticsByDocument.get(documentId));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public FieldStatistics fieldStatistics(FieldName field) {
        Objects.requireNonNull(field, "Field name must not be null");

        readLock.lock();

        try {
            MutableFieldStatistics statistics = statisticsByField.get(field);

            if (statistics == null) {
                return new FieldStatistics(0, 0);
            }

            return statistics.snapshot();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long documentCount() {
        readLock.lock();

        try {
            return statisticsByDocument.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean removeDocument(long documentId) {
        requirePositiveDocumentId(documentId);

        writeLock.lock();

        try {
            DocumentStatistics documentStatistics = statisticsByDocument.remove(documentId);

            if (documentStatistics == null) {
                return false;
            }

            Map<FieldName, Set<String>> documentTerms = termsByDocument.remove(documentId);

            if (documentTerms == null) {
                throw new IllegalStateException(
                        "Missing term metadata for indexed document " + documentId
                 );
            }
            for (Map.Entry<FieldName, Integer> fieldEntry : documentStatistics.fieldLengths().entrySet()) {

                FieldName field = fieldEntry.getKey();
                int fieldLength = fieldEntry.getValue();

                removeDocumentPostings(documentId, field, documentTerms.getOrDefault(field, Set.of()));

                MutableFieldStatistics fieldStatistics = statisticsByField.get(field);

                if (fieldStatistics != null) {
                    fieldStatistics.removeDocument(fieldLength);

                    if (fieldStatistics.isEmpty()) {
                        statisticsByField.remove(field);
                    }
                }
            }
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();

        try {
            postingsByField.clear();
            statisticsByDocument.clear();
            termsByDocument.clear();
            statisticsByField.clear();
        } finally {
            writeLock.unlock();
        }
    }

    private PreparedDocument prepare(IndexDocument document) {
        Map<FieldName, Integer> fieldLengths = new LinkedHashMap<>();

        Map<FieldName, Map<String, List<Integer>>> positionsByField = new LinkedHashMap<>();

        for (Map.Entry<FieldName, String> fieldEntry : document.fields().entrySet()){
            FieldName field = fieldEntry.getKey();
            String content = fieldEntry.getValue();

            List<AnalyzedToken> tokens = analyzer.analyze(content);

            fieldLengths.put(field, tokens.size());

            Map<String, List<Integer>> positionsByTerm = collectPositions(tokens);

            positionsByField.put(field, immutablePositions(positionsByTerm));
        }

        DocumentStatistics documentStatistics = new DocumentStatistics(document.documentId(), fieldLengths);

        return new PreparedDocument(
                documentStatistics,
                Collections.unmodifiableMap(positionsByField)
        );
    }

    private static Map<String, List<Integer>> collectPositions(List<AnalyzedToken> tokens) {
        Map<String, List<Integer>> positionsByTerm = new LinkedHashMap<>();

        for (AnalyzedToken token : tokens) {
            positionsByTerm
                    .computeIfAbsent(token.term(), igonred -> new ArrayList<>())
                    .add(token.position());
        }

        return positionsByTerm;
    }

    private static Map<String, List<Integer>> immutablePositions(Map<String, List<Integer>> positionsByTerm) {
        Map<String, List<Integer>> copy = new LinkedHashMap<>();

        positionsByTerm.forEach(
                (term, positions)
                        -> copy.put(term, List.copyOf(positions))
        );

        return Collections.unmodifiableMap(copy);
    }

    private void addPreparedDocument(PreparedDocument preparedDocument) {
        long documentId = preparedDocument.statistics().documentId();

        Map<FieldName, Set<String>> documentTerms = new LinkedHashMap<>();

        for (
                Map.Entry<FieldName, Map<String, List<Integer>>> fieldEntry
                : preparedDocument.positionsByField().entrySet()
        ) {
            FieldName field = fieldEntry.getKey();
            Map<String, List<Integer>> positionsByTerm = fieldEntry.getValue();

            int fieldLength = preparedDocument.statistics().fieldLength(field);

            statisticsByField
                    .computeIfAbsent(field, ignored -> new MutableFieldStatistics())
                    .addDocument(fieldLength);

            Map<String, NavigableMap<Long, Posting>> fieldPostings =
                    postingsByField.computeIfAbsent(field, ignored -> new HashMap<>());

            for (
                    Map.Entry<String, List<Integer>> termEntry
                    : positionsByTerm.entrySet()
            ) {
                String term = termEntry.getKey();

                NavigableMap<Long, Posting> termPostings =
                        fieldPostings.computeIfAbsent(term, ignored -> new TreeMap<>());

                termPostings.put(documentId, new Posting(documentId, termEntry.getValue()));


            }

            documentTerms.put(field, Set.copyOf(positionsByTerm.keySet()));
        }
        statisticsByDocument.put(documentId, preparedDocument.statistics());

        termsByDocument.put(documentId, Collections.unmodifiableMap(documentTerms));
    }

    private void removeDocumentPostings(long documentId, FieldName field, Set<String> terms) {
        Map<String, NavigableMap<Long, Posting>> fieldPostings = postingsByField.get(field);

        if (fieldPostings == null) {
            return;
        }

        for (String term : terms) {
            NavigableMap<Long, Posting> termPostings = fieldPostings.get(term);

            if (termPostings == null) {
                continue;
            }

            termPostings.remove(documentId);

            if (termPostings.isEmpty()) {
                fieldPostings.remove(term);
            }
        }

        if (fieldPostings.isEmpty()) {
            postingsByField.remove(field);
        }
    }

    private static void requireTerm(String normalizedTerm) {
        if (normalizedTerm == null || normalizedTerm.isBlank()) {
            throw new IllegalArgumentException(
                    "Normalized term must not be blank"
            );
        }
    }

    private static void requirePositiveDocumentId(long documentId) {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }
    }

    private record PreparedDocument(
            DocumentStatistics statistics,
            Map<FieldName, Map<String, List<Integer>>> positionsByField
    ) {}

    private static final class MutableFieldStatistics {
        private long documentsWithField;
        private long totalTokenCount;

        private void addDocument(int fieldLength) {
            documentsWithField++;
            totalTokenCount += fieldLength;
        }

        private void removeDocument(int fieldLength) {
            documentsWithField--;
            totalTokenCount -= fieldLength;
        }

        private boolean isEmpty() {
            return documentsWithField == 0;
        }

        private FieldStatistics snapshot() {
            return new FieldStatistics(documentsWithField, totalTokenCount);
        }
    }

}
