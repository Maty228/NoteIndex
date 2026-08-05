package cz.martim12.noteindex.cli;

import cz.martim12.noteindex.cli.runtime.NoteIndexServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliApplicationTest {

    private ByteArrayOutputStream outputBytes;
    private ByteArrayOutputStream errorBytes;
    private PrintStream output;
    private PrintStream error;

    @BeforeEach
    void setUp() {
        outputBytes = new ByteArrayOutputStream();
        errorBytes = new ByteArrayOutputStream();

        output = new PrintStream(
                outputBytes,
                true,
                StandardCharsets.UTF_8
        );

        error = new PrintStream(
                errorBytes,
                true,
                StandardCharsets.UTF_8
        );
    }

    @Test
    void displaysHelpWhenNoArgumentsAreProvided() {
        CliApplication cli = new CliApplication();

        int exitCode = cli.run(
                new String[0],
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertTrue(
                standardOutput().contains(
                        "NoteIndex command-line interface"
                )
        );

        assertTrue(
                standardOutput().contains(
                        "Usage:"
                )
        );

        assertTrue(errorOutput().isEmpty());
    }

    @Test
    void supportsHelpAliases() {
        CliApplication cli = new CliApplication();

        assertEquals(
                CliExitCode.SUCCESS,
                cli.run(
                        new String[]{"help"},
                        output,
                        error
                )
        );

        outputBytes.reset();

        assertEquals(
                CliExitCode.SUCCESS,
                cli.run(
                        new String[]{"--help"},
                        output,
                        error
                )
        );

        outputBytes.reset();

        assertEquals(
                CliExitCode.SUCCESS,
                cli.run(
                        new String[]{"-h"},
                        output,
                        error
                )
        );
    }

    @Test
    void displaysConfiguredVersion() {
        CliApplication cli = new CliApplication(
                databaseFile -> {
                    throw new AssertionError(
                            "Version must not open the service"
                    );
                },
                "2.5.0-test"
        );

        int exitCode = cli.run(
                new String[]{"--version"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                "NoteIndex 2.5.0-test",
                standardOutput().strip()
        );

        assertTrue(errorOutput().isEmpty());
    }

    @Test
    void returnsUsageErrorForUnknownCommand() {
        CliApplication cli = new CliApplication();

        int exitCode = cli.run(
                new String[]{"unknown"},
                output,
                error
        );

        assertEquals(
                CliExitCode.USAGE_ERROR,
                exitCode
        );

        assertTrue(standardOutput().isEmpty());

        assertTrue(
                errorOutput().contains(
                        "Unknown command: unknown"
                )
        );

        assertTrue(
                errorOutput().contains(
                        "noteindex help"
                )
        );
    }

    @Test
    void returnsUsageErrorForUnexpectedArguments() {
        CliApplication cli = new CliApplication();

        int exitCode = cli.run(
                new String[]{"help", "extra"},
                output,
                error
        );

        assertEquals(
                CliExitCode.USAGE_ERROR,
                exitCode
        );

        assertTrue(
                errorOutput().contains(
                        "Expected one command"
                )
        );
    }

    @Test
    void helpAndVersionDoNotOpenApplicationService() {
        AtomicInteger openingCount =
                new AtomicInteger();

        NoteIndexServiceFactory serviceFactory =
                databaseFile -> {
                    openingCount.incrementAndGet();

                    throw new AssertionError(
                            "Service should not be opened"
                    );
                };

        CliApplication cli = new CliApplication(
                serviceFactory,
                "1.0-test"
        );

        cli.run(
                new String[]{"help"},
                output,
                error
        );

        cli.run(
                new String[]{"version"},
                output,
                error
        );

        assertEquals(0, openingCount.get());
    }

    private String standardOutput() {
        return outputBytes.toString(
                StandardCharsets.UTF_8
        );
    }

    private String errorOutput() {
        return errorBytes.toString(
                StandardCharsets.UTF_8
        );
    }
}