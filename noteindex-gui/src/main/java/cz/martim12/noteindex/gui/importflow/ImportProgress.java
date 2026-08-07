package cz.martim12.noteindex.gui.importflow;

import java.nio.file.Path;
import java.util.Objects;

public record ImportProgress(
        int current,
        int total,
        Path source
) {
    public ImportProgress {
        if (current <= 0) {
            throw new IllegalArgumentException("Current must be greater than zero");
        }

        if (total <= 0) {
            throw new IllegalArgumentException("Total must be greater than zero");
        }
        if (current > total) {
            throw new IllegalArgumentException("Current must be less than or equal to total");
        }

        Objects.requireNonNull(source, "Source path must not be null");
    }

    public double fraction() {
        return (double) (current - 1) / total;
    }
}
