package cz.martim12.noteindex.gui.importflow;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents progress information for a single imported file.
 *
 * @param current one-based position of the current file
 * @param total total number of files being imported
 * @param source current file being processed
 */
public record ImportProgress(
        int current,
        int total,
        Path source
) {
    /**
     * Creates validated import progress information.
     *
     * @param current one-based position of the current file
     * @param total total number of files being imported
     * @param source current file being processed
     * @throws IllegalArgumentException if progress values are invalid
     * @throws NullPointerException if the source path is null
     */
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

    /**
     * Calculates the progress fraction before completing the current file.
     *
     * @return progress value between zero and one
     */
    public double fraction() {
        return (double) (current - 1) / total;
    }
}
