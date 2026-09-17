package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexReader;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.query.QueryPhrase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Matches phrases by intersecting positional posting lists.
 */
public final class PositionalPhraseMatcher implements PhraseMatcher {

    private final IndexReader indexReader;

    /**
     * Creates a positional phrase matcher.
     *
     * @param indexReader source of positional index data
     */
    public PositionalPhraseMatcher(IndexReader indexReader) {
        this.indexReader = Objects.requireNonNull(indexReader, "Index reader must not be null");
    }

    /**
     * Finds all occurrences of a phrase in the selected fields.
     *
     * @param phrase phrase to match
     * @param fields searchable fields
     * @return matching phrase occurrences
     */
    @Override
    public List<PhraseMatch> findMatches(QueryPhrase phrase, Collection<FieldName> fields) {
        Objects.requireNonNull(phrase, "Query phrase must not be null");

        List<FieldName> searchableFields = copyFields(fields);
        List<PhraseMatch> matches = new ArrayList<>();

        for (FieldName field : searchableFields) {
            matches.addAll(findMatchesInField(phrase, field));
        }

        matches.sort(
                Comparator.comparingLong(PhraseMatch::documentId)
                        .thenComparing(
                                match -> match.field().value()
                        )
        );

        return List.copyOf(matches);
    }

    /** Finds phrase occurrences in one field by aligning positional postings. */
    private List<PhraseMatch> findMatchesInField(QueryPhrase phrase, FieldName field) {
        List<Map<Long, Posting>> postingsByTerm = new ArrayList<>();

        for (String term : phrase.terms()) {
            List<Posting> postings = indexReader.postings(term, field);

            if (postings.isEmpty()) {
                return List.of();
            }

            postingsByTerm.add(mapByDocument(postings));
        }

        NavigableSet<Long> commonDocumentIds = new TreeSet<>(postingsByTerm.getFirst().keySet());

        for (int index = 1; index < postingsByTerm.size(); index++) {
            commonDocumentIds.retainAll(postingsByTerm.get(index).keySet());

            if (commonDocumentIds.isEmpty()) {
                return List.of();
            }
        }

        List<PhraseMatch> matches = new ArrayList<>();

        for (long documentId : commonDocumentIds) {
            List<Integer> validStarts = new ArrayList<>(
                    postingsByTerm
                            .getFirst()
                            .get(documentId)
                            .positions()
            );

            for (int termOffset = 1; termOffset < postingsByTerm.size(); termOffset++) {
                List<Integer> nextPositions = postingsByTerm
                        .get(termOffset)
                        .get(documentId)
                        .positions();

                validStarts = retailAlignedStarts(validStarts, nextPositions, termOffset);

                if (validStarts.isEmpty()) {
                    break;
                }
            }

            if (!validStarts.isEmpty()) {
                matches.add(
                        new PhraseMatch(documentId, field, validStarts)
                );
            }

        }

        return matches;
    }

    /** Indexes a term's postings by document and rejects duplicate entries. */
    private static Map<Long, Posting> mapByDocument(List<Posting> postings) {
        Map<Long, Posting> byDocument = new HashMap<>();

        for (Posting posting : postings) {
            Posting previous = byDocument.put(posting.documentId(), posting);

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate posting for document " + posting.documentId()
                );
            }

        }

        return byDocument;
    }

    /**
     * Retains starts for which the next phrase term occurs at
     * start + termOffset.
     *
     * @param currentStarts candidate positions of the phrase's first term
     * @param nextPositions positions of the term being aligned
     * @param termOffset required offset from each candidate start
     * @return candidate starts that remain positionally aligned
     */
    private static List<Integer> retailAlignedStarts(List<Integer> currentStarts, List<Integer> nextPositions, int termOffset) {
        List<Integer> retained = new ArrayList<>();

        int startIndex = 0;
        int nextIndex = 0;

        while (startIndex < currentStarts.size() && nextIndex < nextPositions.size()) {
            int start = currentStarts.get(startIndex);
            int expectedPositions = start + termOffset;
            int actualPosition = nextPositions.get(nextIndex);

            if (actualPosition == expectedPositions) {
                retained.add(start);
                startIndex++;
                nextIndex++;
            } else if (actualPosition < expectedPositions) {
                nextIndex++;
            } else {
                startIndex++;
            }
        }

        return retained;
    }

    /** Validates, deduplicates and defensively copies searchable fields. */
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
