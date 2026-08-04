package cz.martim12.noteindex.application.index;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;

import java.util.Map;
import java.util.Objects;

/**
 * Converts persisted documents into the representation consumed
 * by the search index.
 */
public final class DocumentIndexMapper {

    public IndexDocument map(Document document) {
        Objects.requireNonNull(document, "Document must not be null");

        return new IndexDocument(document.id(), Map.of(FieldName.TITLE, document.title(), FieldName.BODY, document.searchableContent()));
    }
}
