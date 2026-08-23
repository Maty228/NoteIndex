package cz.martim12.noteindex.cli.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Command for displaying general or command-specific help.
 *
 * @param topic optional command name for which help is requested
 */
public record HelpCommand(Optional<String> topic) implements CliCommand {

    /**
     * Creates a validated help command.
     *
     * @param topic optional command-specific help topic
     * @throws IllegalArgumentException if the help topic is blank
     */
    public HelpCommand {
        topic = Objects.requireNonNull(topic, "help topic must not be null").map(String::trim);

        if (topic.isPresent() && topic.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("help topic must not be blank");
        }
    }

    /**
     * Creates a command requesting general CLI help.
     *
     * @return general help command
     */
    public static HelpCommand general() {
        return new HelpCommand(Optional.empty());
    }

    /**
     * Creates a command requesting help for a specific command.
     *
     * @param command command name
     * @return command-specific help request
     */
    public static HelpCommand forCommand(String command) {
        return new HelpCommand(Optional.of(Objects.requireNonNull(command, "Help command must not be null")));
    }
}
