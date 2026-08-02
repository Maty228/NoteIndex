package cz.martim12.noteindex.core.model;

import java.time.Instant;
public record DocumentSummary(
        long id,
        String title,
        String format,
        Instant importedAt
) {
}
