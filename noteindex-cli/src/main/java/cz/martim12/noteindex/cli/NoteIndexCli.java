package cz.martim12.noteindex.cli;

/**
 * Executable NoteIndex CLI entry point.
 */
public final class NoteIndexCli {

    /**
     * Utility class constructor.
     */
    private NoteIndexCli() {}

    /**
     * Starts the NoteIndex command-line application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CliApplication().run(args, System.out, System.err);

        System.exit(exitCode);
    }
}
