package cz.martim12.noteindex.gui.application;

import java.nio.file.Path;

/**
 * Resolves filesystem locations used by the desktop GUI.
 */
public final class GuiDatabasePaths {

    private GuiDatabasePaths() {}

    public static Path defaultDatabaseFile() {
        String userHome = System.getProperty("user.home");

        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("Could not determine user home directory");
        }
        return Path.of(userHome, ".noteindex", "noteindex.db").toAbsolutePath().normalize();
    }
}
