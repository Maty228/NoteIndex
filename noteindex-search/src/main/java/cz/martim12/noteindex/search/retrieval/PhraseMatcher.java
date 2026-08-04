package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.query.QueryPhrase;

import java.util.Collection;
import java.util.List;

/**
 * Finds exact positional phrase occurrences.
 */
@FunctionalInterface
public interface PhraseMatcher {

    /**
     * Finds phrase occurrences in the selected searchable fields.
     */
    List<PhraseMatch> findMatches(QueryPhrase phrase, Collection<FieldName> fields);
}
