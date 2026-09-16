package cz.martim12.noteindex.application.support;

import cz.martim12.noteindex.search.index.DocumentStatistics;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.FieldStatistics;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.index.SearchIndex;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Search-index test double that can fail selected mutation calls.
 */
public final class ControllableSearchIndex implements SearchIndex {

    private final SearchIndex delegate;
    private final Deque<RuntimeException> indexFailures = new ArrayDeque<>();
    private final Deque<RuntimeException> removalFailures = new ArrayDeque<>();

    public ControllableSearchIndex(SearchIndex delegate) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "Delegate must not be null"
        );
    }

    public void failNextIndexWith(RuntimeException failure) {
        indexFailures.addLast(
                Objects.requireNonNull(failure, "Failure must not be null")
        );
    }

    public void failNextRemovalWith(RuntimeException failure) {
        removalFailures.addLast(
                Objects.requireNonNull(failure, "Failure must not be null")
        );
    }

    @Override
    public void indexDocument(IndexDocument document) {
        if (!indexFailures.isEmpty()) {
            throw indexFailures.removeFirst();
        }

        delegate.indexDocument(document);
    }

    @Override
    public boolean removeDocument(long documentId) {
        if (!removalFailures.isEmpty()) {
            throw removalFailures.removeFirst();
        }

        return delegate.removeDocument(documentId);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public List<Posting> postings(
            String normalizedTerm,
            FieldName field
    ) {
        return delegate.postings(normalizedTerm, field);
    }

    @Override
    public List<String> termsWithPrefix(
            String normalizedPrefix,
            FieldName field
    ) {
        return delegate.termsWithPrefix(normalizedPrefix, field);
    }

    @Override
    public Optional<DocumentStatistics> documentStatistics(
            long documentId
    ) {
        return delegate.documentStatistics(documentId);
    }

    @Override
    public FieldStatistics fieldStatistics(FieldName field) {
        return delegate.fieldStatistics(field);
    }

    @Override
    public long documentCount() {
        return delegate.documentCount();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
