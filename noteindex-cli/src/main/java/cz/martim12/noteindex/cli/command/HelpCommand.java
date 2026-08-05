package cz.martim12.noteindex.cli.command;

import java.util.Objects;
import java.util.Optional;

public record HelpCommand(Optional<String> topic) implements CliCommand {

    public HelpCommand {
        topic = Objects.requireNonNull(topic, "help topic must not be null").map(String::trim);

        if (topic.isPresent() && topic.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("help topic must not be blank");
        }
    }

    public static HelpCommand general() {
        return new HelpCommand(Optional.empty());
    }

    public static HelpCommand forCommand(String command) {
        return new HelpCommand(Optional.of(Objects.requireNonNull(command, "Help command must not be null")));
    }
}
