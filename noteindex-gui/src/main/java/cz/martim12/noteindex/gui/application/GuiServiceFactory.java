package cz.martim12.noteindex.gui.application;

import cz.martim12.noteindex.application.api.NoteIndexService;

import java.nio.file.Path;

/**
 * Opens the application service used by the GUI.
 * The abstraction keeps GUI lifecycle tests independent from
 * a real SQLite database.
 */
@FunctionalInterface
public interface GuiServiceFactory {

    /**
     * Opens the application service for the selected database.
     *
     * @param databaseFile database file location
     * @return opened application service
     */
    NoteIndexService open(Path databaseFile);
}
