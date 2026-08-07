package cz.martim12.noteindex.gui.application;

/**
 * Lifecycle state of the application runtime owned by the GUI.
 */
public enum GuiLifecycleState {
    NEW,
    STARTING,
    READY,
    FAILED,
    CLOSED
}
