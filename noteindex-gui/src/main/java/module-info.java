/**
 * Provides the JavaFX graphical user interface for NoteIndex.
 *
 * <p>This module contains the desktop application, views, controllers,
 * and GUI-specific workflows.</p>
 */
module noteindex.gui {
    requires javafx.controls;
    requires noteindex.application;
    requires java.prefs;

    exports cz.martim12.noteindex.gui;
}