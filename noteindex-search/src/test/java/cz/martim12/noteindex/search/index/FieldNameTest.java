package cz.martim12.noteindex.search.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldNameTest {

    @Test
    void normalizesFieldName() {
        FieldName fieldName = new FieldName("  OCR_Text ");

        assertEquals("ocr_text", fieldName.value());
    }

    @Test
    void acceptsFutureCustomFields() {
        assertEquals(
                "formula",
                new FieldName("formula").value()
        );
    }

    @Test
    void rejectsInvalidFieldName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FieldName("page content")
        );
    }
}
