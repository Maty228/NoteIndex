package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexReader;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryPhrase;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Retrieves candidates using ranked-OR terms and required phrases.
 *
 * Standalone terms:
 *   candidates are the union of matching documents.
 *
 * Required phrases:
 *   candidates must match every phrase.
 *
 * In a mixed query, standalone terms are optional ranking signals,
 * while quoted phrases remain mandatory.
 */
public final class DefaultCandidateRetriever implements CandidateRetriever {

    private final IndexReader indexReader;
    private final PhraseMatcher phraseMatcher;
    private final List<FieldName> fields;

    private final StandaloneTermMatchMode standaloneTermMatchMode;


    public DefaultCandidateRetriever(
            IndexReader indexReader,
            PhraseMatcher phraseMatcher,
            Collection<FieldName> fields
    ) {
        this(
                indexReader,
                phraseMatcher,
                fields,
                StandaloneTermMatchMode.EXACT
        );
    }
    public DefaultCandidateRetriever(IndexReader indexReader, PhraseMatcher phraseMatcher, Collection<FieldName> fields, StandaloneTermMatchMode standaloneTermMatchMode) {
        this.indexReader = Objects.requireNonNull(
                indexReader,
                "Index reader must not be null"
        );

        this.phraseMatcher = Objects.requireNonNull(
                phraseMatcher,
                "Phrase matcher must not be null"
        );

        this.fields = copyFields(fields);

        this.standaloneTermMatchMode = Objects.requireNonNull(
                standaloneTermMatchMode,
                "Standalone term match mode must not be null"
        );
    }

    @Override
    public List<Long> retrieveCandidates(ParsedQuery query) {
        Objects.requireNonNull(query, "Parsed must not be null");

        if (query.hasRequiredPhrases()) {
            return retrieveRequiredPhraseCandidates(query);
        }

        return retrieveTermCandidates(query);
    }

    private List<Long> retrieveTermCandidates(ParsedQuery query) {
        NavigableSet<Long> candidates = new TreeSet<>();

        for (String queryTerm : query.terms()) {
            for (FieldName field : fields) {
                for (String indexedTerm :
                        matchingTerms(queryTerm, field)) {

                    indexReader.postings(indexedTerm, field)
                            .stream()
                            .map(Posting::documentId)
                            .forEach(candidates::add);
                }
            }
        }

        return List.copyOf(candidates);
    }

    private List<String> matchingTerms(
            String queryTerm,
            FieldName field
    ) {
        return switch (standaloneTermMatchMode) {
            case EXACT -> List.of(queryTerm);

            case PREFIX ->
                    indexReader.termsWithPrefix(
                            queryTerm,
                            field
                    );
        };
    }

    private List<Long> retrieveRequiredPhraseCandidates(ParsedQuery query) {
        NavigableSet<Long> candidates = null;

        for (QueryPhrase phrase : query.requiredPhrases()) {
            NavigableSet<Long> phraseDocumentIds = new TreeSet<>();

            phraseMatcher.findMatches(phrase, fields)
                    .stream()
                    .map(PhraseMatch::documentId)
                    .forEach(phraseDocumentIds::add);

            if (candidates == null) {
                candidates = phraseDocumentIds;
            } else {
                candidates.retainAll(phraseDocumentIds);
            }

            if (candidates.isEmpty()) {
                return List.of();
            }
        }

        return candidates == null ? List.of() : List.copyOf(candidates);
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
