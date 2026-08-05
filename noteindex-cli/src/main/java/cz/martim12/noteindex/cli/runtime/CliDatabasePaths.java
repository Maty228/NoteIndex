package cz.martim12.noteindex.cli.runtime;

import java.nio.file.Path;

/**
 * Resolves standard filesystem locations used by the CLI.
 */
public final class CliDatabasePaths {
    private CliDatabasePaths() {}

    public static Path defaultDatabaseFile() {
        String userHome = System.getProperty("user.home");

        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("Could not determine user home directory");
        }
        return Path.of(userHome, ".noteindex", "noteindex.db").toAbsolutePath().normalize();
    }
}
