/**
 * Provides document importing functionality.
 *
 * <p>This module defines importer APIs, importer discovery, and built-in
 * support for supported document formats.</p>
 */
module noteindex.importer {
    requires transitive noteindex.core;

    exports cz.martim12.noteindex.importer.api;
    exports cz.martim12.noteindex.importer.annotation;
    exports cz.martim12.noteindex.importer.exception;
    exports cz.martim12.noteindex.importer.registry;

    uses cz.martim12.noteindex.importer.api.DocumentImporter;

    provides cz.martim12.noteindex.importer.api.DocumentImporter
            with cz.martim12.noteindex.importer.builtin.TxtDocumentImporter,
                 cz.martim12.noteindex.importer.builtin.MarkdownDocumentImporter;
}