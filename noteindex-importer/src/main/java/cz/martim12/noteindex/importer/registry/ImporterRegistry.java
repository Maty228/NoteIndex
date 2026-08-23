package cz.martim12.noteindex.importer.registry;

import cz.martim12.noteindex.importer.annotation.ImporterPlugin;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.exception.ImporterConfigurationException;
import cz.martim12.noteindex.importer.exception.UnsupportedFormatException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry responsible for discovering and resolving document importers.
 *
 * <p>Importers are selected based on normalized file extensions.</p>
 */
public class ImporterRegistry {
    private final Map<String, ImporterDefinition> importersByExtension;
    private final List<ImporterDefinition> definitions;

    /**
     * Creates a registry from available importer implementations.
     *
     * @param importers importer implementations to register
     * @throws ImporterConfigurationException if importers contain invalid
     *         metadata or conflicting extensions
     */
    public ImporterRegistry(Iterable<DocumentImporter> importers) {
        Map<String, ImporterDefinition> registered = new LinkedHashMap<>();
        List<ImporterDefinition> discoveredDefinitions = new ArrayList<>();

        for (DocumentImporter importer : importers){
            ImporterDefinition definition = createDefinition(importer);
            discoveredDefinitions.add(definition);

            for (String extension : definition.extensions()){
                ImporterDefinition previous = registered.putIfAbsent(extension, definition);

                if (previous != null){
                    throw new ImporterConfigurationException("Multiple importers support extension '%s': %s and %s"
                            .formatted(
                                    extension,
                                    previous.name(),
                                    definition.name()
                            )
                    );
                }
            }
        }

        this.importersByExtension = Map.copyOf(registered);
        this.definitions = List.copyOf(discoveredDefinitions);
    }

    /**
     * Discovers available importers using Java's service loader mechanism.
     *
     * @return registry containing discovered importers
     */
    public static ImporterRegistry discover() {
        ServiceLoader<DocumentImporter> loader = ServiceLoader.load(DocumentImporter.class);
        return new ImporterRegistry(loader);
    }

    /**
     * Finds an importer capable of handling the given file.
     *
     * @param source source file
     * @return matching importer
     * @throws UnsupportedFormatException if no importer supports the file extension
     */
    public DocumentImporter resolve(Path source) {
        String extension = extractExtension(source);
        ImporterDefinition definition = importersByExtension.get(extension);

        if (definition == null){
            throw new UnsupportedFormatException(source);
        }

        return definition.importer();
    }

    /**
     * Returns all supported file extensions.
     *
     * @return supported extensions
     */
    public Set<String> supportedExtensions() {
        return importersByExtension.keySet();
    }

    /**
     * Returns registered importer definitions.
     *
     * @return registered importers
     */
    public Collection<ImporterDefinition> importers() {
        return definitions;
    }

    private static ImporterDefinition createDefinition(DocumentImporter importer) {
        Class<?> importerClass = importer.getClass();

        ImporterPlugin annotation = importerClass.getAnnotation(ImporterPlugin.class);

        if (annotation == null){
            throw new ImporterConfigurationException(
                    "Importer %s is missing @ImporterPlugin"
                            .formatted(importerClass.getName())
            );
        }

        String name = requireText(annotation.name(), "name", importerClass);
        String formatId = requireText(annotation.formatId(), "formatId", importerClass);

        Set<String> extensions = Arrays.stream(annotation.extensions())
                .map(ImporterRegistry::normalizeExtension)
                .filter(extension -> !extension.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        if (extensions.isEmpty()){
            throw new ImporterConfigurationException(
                    "Importer %s declares no valid extensions"
                            .formatted(importerClass.getName())
            );
        }

        return new ImporterDefinition(name, formatId, extensions, importer);
    }

    private static String extractExtension(Path source){
        Path fileNamePath = source.getFileName();

        if (fileNamePath == null){
            throw new UnsupportedFormatException(source);
        }

        String fileName = fileNamePath.toString();
        int separator = fileName.lastIndexOf('.');

        if (separator < 0 || separator == fileName.length() - 1) {
            throw new UnsupportedFormatException(source);
        }
        return normalizeExtension(fileName.substring(separator + 1));
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private static String requireText(String value, String property, Class<?> importerClass) {
        if (value == null || value.isBlank()) {
            throw new ImporterConfigurationException(
                    "Importer %s has an empty annotation property '%s'"
                            .formatted(importerClass.getName(), property)
            );
        }

        return value.trim();
    }
}
