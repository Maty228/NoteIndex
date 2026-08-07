package cz.martim12.noteindex.gui.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiDatabasePathsTest {

    @Test
    void resolvesDatabaseInsideUserHome() {
        Path expected = Path.of(
                System.getProperty("user.home"),
                ".noteindex",
                "noteindex.db"
        ).toAbsolutePath().normalize();

        assertEquals(
                expected,
                GuiDatabasePaths.defaultDatabaseFile()
        );
    }
}