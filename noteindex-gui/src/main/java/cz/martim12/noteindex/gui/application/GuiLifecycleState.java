package cz.martim12.noteindex.gui.application;

/**
 * Lifecycle state of the application runtime owned by the GUI.
 */
public enum GuiLifecycleState {
    /**
     * Application has not started yet.
     */
    NEW,
    /**
     * Application has started but initialization is still in progress.
     */
    STARTING,
    /**
     * Application initialization completed successfully.
     */
    READY,
    /**
     * Application startup failed due to an unrecoverable error.
     */
    FAILED,
    /**
     * Application has been closed and its resources have been released.
     */
    CLOSED
}
