package cz.martim12.noteindex.cli.error;

import cz.martim12.noteindex.cli.CliExitCode;
import cz.martim12.noteindex.cli.parsing.CliUsageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliErrorHandlerTest {

    private CliErrorHandler errorHandler;
    private ByteArrayOutputStream errorBytes;
    private PrintStream errorOutput;

    @BeforeEach
    void setUp() {
        errorHandler = new CliErrorHandler();
        errorBytes = new ByteArrayOutputStream();

        errorOutput = new PrintStream(
                errorBytes,
                true,
                StandardCharsets.UTF_8
        );
    }

    @Test
    void handlesUsageErrors() {
        int exitCode = errorHandler.handleUsageError(
                new CliUsageException(
                        "Unknown command: invalid"
                ),
                errorOutput
        );

        assertEquals(
                CliExitCode.USAGE_ERROR,
                exitCode
        );

        String displayed = displayedError();

        assertTrue(
                displayed.contains(
                        "Error: Unknown command: invalid"
                )
        );

        assertTrue(
                displayed.contains(
                        "Run 'noteindex help' for usage."
                )
        );
    }

    @Test
    void handlesOperationErrors() {
        int exitCode = errorHandler.handleOperationError(
                new IllegalStateException(
                        "Database unavailable"
                ),
                errorOutput
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertEquals(
                "Error: Database unavailable",
                displayedError().strip()
        );
    }

    @Test
    void usesCauseMessageWhenOuterMessageIsBlank() {
        RuntimeException exception =
                new RuntimeException(
                        null,
                        new IllegalStateException(
                                "Underlying failure"
                        )
                );

        int exitCode = errorHandler.handleOperationError(
                exception,
                errorOutput
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertEquals(
                "Error: Underlying failure",
                displayedError().strip()
        );
    }

    @Test
    void fallsBackToExceptionClassName() {
        int exitCode = errorHandler.handleOperationError(
                new IllegalStateException(),
                errorOutput
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertEquals(
                "Error: IllegalStateException",
                displayedError().strip()
        );
    }

    private String displayedError() {
        return errorBytes.toString(
                StandardCharsets.UTF_8
        );
    }
}