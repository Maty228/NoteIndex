package cz.martim12.noteindex.cli;

/**
 * Executable NoteIndex CLI entry point.
 */
public final class NoteIndexCli {

    private NoteIndexCli() {}

    public static void main(String[] args) {
        int exitCode = new CliApplication().run(args, System.out, System.err);

        System.exit(exitCode);
    }
}
