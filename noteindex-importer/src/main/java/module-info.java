module noteindex.importer {
    requires noteindex.core;

    exports cz.martim12.noteindex.importer.api;
    exports cz.martim12.noteindex.importer.annotation;
    exports cz.martim12.noteindex.importer.exception;
    exports cz.martim12.noteindex.importer.registry;

    uses cz.martim12.noteindex.importer.api.DocumentImporter;

    provides cz.martim12.noteindex.importer.api.DocumentImporter
            with cz.martim12.noteindex.importer.builtin.TxtDocumentImporter,
                 cz.martim12.noteindex.importer.builtin.MarkdownDocumentImporter;
}