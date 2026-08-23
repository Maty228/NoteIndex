package cz.martim12.noteindex.cli;

/**
 * Process exit codes used by the NoteIndex CLI.
 */
public final class CliExitCode {

    /**
     * Successful command execution.
     */
    public static final int SUCCESS = 0;
    /**
     * Command execution failed.
     */
    public static final int FAILURE = 1;
    /**
     * Invalid command usage.
     */
    public static final int USAGE_ERROR = 2;

    /**
     * Utility class constructor.
     */
    private CliExitCode() {}
}
