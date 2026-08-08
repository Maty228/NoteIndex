package cz.martim12.noteindex.search.query;

/**
 * Controls how standalone query terms are matched against indexed
 * vocabulary.
 *
 * Required quoted phrases remain exact regardless of this setting.
 */
public enum StandaloneTermMatchMode {

    /**
     * A standalone query term matches only the identical normalized
     * indexed term.
     */
    EXACT,

    /**
     * A standalone query term matches every normalized indexed term
     * beginning with that query term.
     */
    PREFIX
}
