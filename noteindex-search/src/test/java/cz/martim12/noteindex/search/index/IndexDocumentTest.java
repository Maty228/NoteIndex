package cz.martim12.noteindex.search.index;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexDocumentTest {

    @Test
    void storesNamedSearchableFields() {
        IndexDocument document = new IndexDocument(
                42,
                Map.of(
                        FieldName.TITLE,
                        "Binary Trees",
                        FieldName.BODY,
                        "AVL trees remain balanced."
                )
        );

        assertEquals(
                "Binary Trees",
                document.field(FieldName.TITLE).orElseThrow()
        );

        assertEquals(
                "AVL trees remain balanced.",
                document.field(FieldName.BODY).orElseThrow()
        );
    }

    @Test
    void supportsCustomFields() {
        FieldName formula = new FieldName("formula");

        IndexDocument document = new IndexDocument(
                1,
                Map.of(formula, "O(log n)")
        );

        assertEquals(
                "O(log n)",
                document.field(formula).orElseThrow()
        );
    }

    @Test
    void createsDefensiveCopyOfFields() {
        Map<FieldName, String> fields = new LinkedHashMap<>();
        fields.put(FieldName.TITLE, "Original");

        IndexDocument document = new IndexDocument(1, fields);

        fields.put(FieldName.BODY, "Added later");

        assertFalse(document.fields().containsKey(FieldName.BODY));
    }

    @Test
    void exposesUnmodifiableFields() {
        IndexDocument document = new IndexDocument(
                1,
                Map.of(FieldName.TITLE, "Java")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> document.fields().put(
                        FieldName.BODY,
                        "Content"
                )
        );
    }

    @Test
    void rejectsNonPositiveDocumentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndexDocument(
                        0,
                        Map.of(FieldName.TITLE, "Invalid")
                )
        );
    }

    @Test
    void rejectsDocumentWithoutFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndexDocument(1, Map.of())
        );
    }
}
