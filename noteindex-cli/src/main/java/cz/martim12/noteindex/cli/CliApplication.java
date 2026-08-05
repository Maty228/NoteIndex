package cz.martim12.noteindex.cli;


import cz.martim12.noteindex.application.api.NoteIndexApplications;
import cz.martim12.noteindex.cli.runtime.NoteIndexServiceFactory;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Testable command-line application.
 *
 * The main method delegates here so commands can be tested without
 * starting another JVM or replacing System.out and System.err.
 */
public final class CliApplication {

    public static final String VERSION = "1.0";

    private final NoteIndexServiceFactory serviceFactory;
    private final String version;

    public CliApplication() {
        this(NoteIndexApplications::open, VERSION);
    }

    public CliApplication(NoteIndexServiceFactory serviceFactory, String version) {
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "Service factory must not be null");
        this.version = requireNonBlank(version, "CLI version");
    }

    /**
     * Executes one CLI invocation.
     *
     * @return process exit code
     */
    public int run(String[] args, PrintStream standardOutput, PrintStream errorOutput) {
        Objects.requireNonNull(args, "Arguments must not be null");
        Objects.requireNonNull(standardOutput, "Standard output must not be null");
        Objects.requireNonNull(errorOutput, "Error output must not be null");

        if (args.length == 0) {
            printHelp(standardOutput);
            return CliExitCode.SUCCESS;
        }

        if (args.length != 1) {
            printUsageError(errorOutput, "Expected one command");

            return CliExitCode.USAGE_ERROR;
        }

        return switch (args[0]) {
            case "help", "--help", "-h" -> {
                printHelp(standardOutput);
                yield CliExitCode.SUCCESS;
            }

            case "version", "--version", "-v" -> {
                printVersion(standardOutput);
                yield CliExitCode.SUCCESS;
            }

            default -> {
                printUsageError(errorOutput, "Unknown command: " + args[0]);
                yield CliExitCode.USAGE_ERROR;
            }
        };
    }

    private void printHelp(PrintStream output) {
        output.println("""
                NoteIndex command-line interface
                
                Usage:
                  noteindex <command>
                  
                Commands:
                  help        Show this help message
                  version     Show the NoteIndex version
                  
                Options:
                  -h, --help     Show this help message
                  -v, --version  Show the NoteIndex version 
                """.stripTrailing());
    }

    private void printVersion(PrintStream output) {
        output.println("NoteIndex " + version);
    }

    private void printUsageError(PrintStream output, String message) {
        output.println("Error: " + message);
        output.println("Run 'noteindex help' for usage.");
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
