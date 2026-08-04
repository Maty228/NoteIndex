module noteindex.application {
    requires transitive noteindex.core;

    requires noteindex.importer;
    requires noteindex.persistence;
    requires noteindex.search;

    exports cz.martim12.noteindex.application.api;
}