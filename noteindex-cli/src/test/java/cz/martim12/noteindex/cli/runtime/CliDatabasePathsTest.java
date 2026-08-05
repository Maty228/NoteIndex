package cz.martim12.noteindex.cli.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliDatabasePathsTest {

    @Test
    void resolvesDatabaseInsideUserHome() {
        Path expected = Path.of(
                System.getProperty("user.home"),
                ".noteindex",
                "noteindex.db"
        ).toAbsolutePath().normalize();

        assertEquals(
                expected,
                CliDatabasePaths.defaultDatabaseFile()
        );
    }
}