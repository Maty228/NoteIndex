package cz.martim12.noteindex.gui.importflow;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides helper operations for validating files selected for import.
 */
public final class ImportFileSupport {

    private final Set<String> supportedExtensions;

    /**
     * Creates file support utilities.
     *
     * @param supportedExtensions supported file extensions
     */
    public ImportFileSupport(Set<String> supportedExtensions) {
        Objects.requireNonNull(supportedExtensions, "Supported extensions must not be null");

        this.supportedExtensions = supportedExtensions.stream()
                .map(ImportFileSupport::normalizeExtension)
                .filter(extension -> !extension.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks whether a file can be imported.
     *
     * @param path file path to check
     * @return true if the file has a supported format
     */
    public boolean isSupported(Path path) {
        Objects.requireNonNull(path, "Path must not be null");

        if (!Files.isRegularFile(path)) {
            return false;
        }
        String extension = extensionOf(path);


        return !extension.isBlank() && supportedExtensions.contains(extension);
    }

    /**
     * Checks whether a collection contains at least one supported file.
     *
     * @param paths files to inspect
     * @return true if a supported file exists
     */
    public boolean containsSupportedFile(List<Path> paths) {
        Objects.requireNonNull(paths, "Paths must not be null");

        return paths.stream().anyMatch(this::isSupported);
    }

    /**
     * Returns normalized regular files from the provided paths.
     *
     * @param paths paths to filter
     * @return valid regular files
     */
    public List<Path> regularFiles(List<Path> paths) {
        Objects.requireNonNull(paths, "Paths must not be null");

        return paths.stream()
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isRegularFile)
                .distinct()
                .toList();
    }

    /**
     * Returns a display label of supported import formats.
     *
     * @return formatted extension list
     */
    public String supportedFormatsLabel() {
        return supportedExtensions.stream()
                .sorted(Comparator.comparingInt(ImportFileSupport::formatPriority)
                        .thenComparing(extension -> extension))
                .map(String::toUpperCase)
                .collect(Collectors.joining(" · "));

    }

    private static String extensionOf(Path path) {
        Path fileName = path.getFileName();

        if (fileName == null) {
            return "";
        }

        String name = fileName.toString();
        int separator = name.lastIndexOf('.');

        if (separator < 0 || separator == name.length() - 1) {
            return "";
        }

        return normalizeExtension(name.substring(separator + 1));
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith("*.")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private static int formatPriority(String extension) {
        return switch (extension) {
            case "txt" -> 1;
            case "md" -> 2;
            case "markdown" -> 3;
            default -> 100;
        };
    }
}
