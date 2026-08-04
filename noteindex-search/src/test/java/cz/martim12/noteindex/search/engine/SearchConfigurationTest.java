package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.ranking.Bm25Parameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchConfigurationTest {

    @Test
    void providesApplicationDefaults() {
        SearchConfiguration configuration =
                SearchConfiguration.defaults();

        assertEquals(
                List.of(
                        FieldName.TITLE,
                        FieldName.BODY
                ),
                configuration.fields()
        );

        assertEquals(
                Map.of(
                        FieldName.TITLE, 3.0,
                        FieldName.BODY, 1.0
                ),
                configuration.fieldWeights()
        );

        assertEquals(
                Bm25Parameters.DEFAULT,
                configuration.bm25Parameters()
        );

        assertEquals(
                2.0,
                configuration.phraseOccurrenceBonus()
        );
    }

    @Test
    void createsDefensiveCopies() {
        List<FieldName> fields =
                new ArrayList<>(
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        )
                );

        Map<FieldName, Double> weights =
                new LinkedHashMap<>();

        weights.put(FieldName.TITLE, 3.0);
        weights.put(FieldName.BODY, 1.0);

        SearchConfiguration configuration =
                new SearchConfiguration(
                        fields,
                        weights,
                        Bm25Parameters.DEFAULT,
                        2.0
                );

        fields.clear();
        weights.clear();

        assertEquals(
                List.of(
                        FieldName.TITLE,
                        FieldName.BODY
                ),
                configuration.fields()
        );

        assertEquals(2, configuration.fieldWeights().size());
    }

    @Test
    void rejectsMissingOrAdditionalFieldWeights() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchConfiguration(
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        Map.of(FieldName.TITLE, 3.0),
                        Bm25Parameters.DEFAULT,
                        2.0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchConfiguration(
                        List.of(FieldName.BODY),
                        Map.of(
                                FieldName.BODY, 1.0,
                                FieldName.TITLE, 3.0
                        ),
                        Bm25Parameters.DEFAULT,
                        2.0
                )
        );
    }

    @Test
    void rejectsDuplicateFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchConfiguration(
                        List.of(
                                FieldName.BODY,
                                FieldName.BODY
                        ),
                        Map.of(FieldName.BODY, 1.0),
                        Bm25Parameters.DEFAULT,
                        2.0
                )
        );
    }

    @Test
    void rejectsInvalidPhraseBonus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchConfiguration(
                        List.of(FieldName.BODY),
                        Map.of(FieldName.BODY, 1.0),
                        Bm25Parameters.DEFAULT,
                        -1.0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchConfiguration(
                        List.of(FieldName.BODY),
                        Map.of(FieldName.BODY, 1.0),
                        Bm25Parameters.DEFAULT,
                        Double.NaN
                )
        );
    }
}