package cz.martim12.noteindex.application.index;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentIndexMapperTest {

    private final DocumentIndexMapper mapper =
            new DocumentIndexMapper();

    @Test
    void mapsTitleAndSearchableContent() {
        Document document = new Document(
                7,
                "Java Virtual Machine",
                "file:///notes/jvm.txt",
                "txt",
                "Original document representation",
                "Normalized searchable document content",
                Instant.parse("2026-08-04T18:00:00Z")
        );

        IndexDocument indexed = mapper.map(document);

        assertEquals(7, indexed.documentId());

        assertEquals(
                Map.of(
                        FieldName.TITLE,
                        "Java Virtual Machine",
                        FieldName.BODY,
                        "Normalized searchable document content"
                ),
                indexed.fields()
        );
    }

    @Test
    void rejectsNullDocument() {
        assertThrows(
                NullPointerException.class,
                () -> mapper.map(null)
        );
    }
}