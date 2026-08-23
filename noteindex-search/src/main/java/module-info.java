/**
 * Provides full-text search functionality for NoteIndex.
 *
 * <p>This module contains indexing, query processing, candidate retrieval,
 * ranking, and search result generation.</p>
 */
module noteindex.search {
    exports cz.martim12.noteindex.search.analysis;
    exports cz.martim12.noteindex.search.index;
    exports cz.martim12.noteindex.search.query;
    exports cz.martim12.noteindex.search.retrieval;
    exports cz.martim12.noteindex.search.ranking;
    exports cz.martim12.noteindex.search.engine;
    exports cz.martim12.noteindex.search.snippet;
}