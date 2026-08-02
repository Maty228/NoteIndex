package cz.martim12.noteindex.importer.registry;

import cz.martim12.noteindex.importer.api.DocumentImporter;

import java.util.Objects;
import java.util.Set;

public record ImporterDefinition(
        String name,
        String formatId,
        Set<String> extensions,
        DocumentImporter importer
) {
    public ImporterDefinition {
        Objects.requireNonNull(name);
        Objects.requireNonNull(formatId);
        Objects.requireNonNull(extensions);
        Objects.requireNonNull(importer);

        extensions = Set.copyOf(extensions);
    }
}
