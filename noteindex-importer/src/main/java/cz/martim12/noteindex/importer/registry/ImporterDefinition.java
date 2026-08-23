package cz.martim12.noteindex.importer.registry;

import cz.martim12.noteindex.importer.api.DocumentImporter;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a registered document importer.
 *
 * @param name importer display name
 * @param formatId handled format identifier
 * @param extensions supported file extensions
 * @param importer importer instance
 */
public record ImporterDefinition (
        String name,
        String formatId,
        Set<String> extensions,
        DocumentImporter importer
) {
    /**
     * Creates an importer definition and validates its required values.
     *
     * @param name importer display name
     * @param formatId handled format identifier
     * @param extensions supported file extensions
     * @param importer importer instance
     */
    public ImporterDefinition {
        Objects.requireNonNull(name);
        Objects.requireNonNull(formatId);
        Objects.requireNonNull(extensions);
        Objects.requireNonNull(importer);

        extensions = Set.copyOf(extensions);
    }
}
