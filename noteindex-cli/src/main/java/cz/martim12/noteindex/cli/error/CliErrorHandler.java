package cz.martim12.noteindex.cli.error;

import cz.martim12.noteindex.cli.CliExitCode;
import cz.martim12.noteindex.cli.output.CliOutputFormatter;
import cz.martim12.noteindex.cli.parsing.CliUsageException;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Converts expected CLI failures into user-facing messages
 * and process exit codes.
 *
 * Stack traces are deliberately not printed for normal command
 * failures.
 */
public final class CliErrorHandler {

    public int handleUsageError(CliUsageException exception, PrintStream errorOutput) {
        Objects.requireNonNull(exception, "Usage exception must not be null");
        Objects.requireNonNull(errorOutput, "Error output must not be null");

        CliOutputFormatter.printUsageError(errorOutput, displayMessage(exception));

        return CliExitCode.USAGE_ERROR;
    }

    public int handleOperationError(Throwable exception, PrintStream errorOutput) {
        Objects.requireNonNull(exception, "Operation exception must not be null");
        Objects.requireNonNull(errorOutput, "Error output must not be null");

        CliOutputFormatter.printOperationError(errorOutput, displayMessage(exception));

        return CliExitCode.FAILURE;
    }

    static String displayMessage(Throwable exception) {
        Objects.requireNonNull(exception, "Exception must not be null");
        Throwable current = exception;

        while (current != null) {
            String message = current.getMessage();

            if (message != null && !message.isBlank()) {
                return message;
            }

            Throwable cause = current.getCause();

            if (cause == current) {
                break;
            }
            current = cause;
        }

        return exception.getClass().getSimpleName();
    }
}
