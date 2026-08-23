package cz.martim12.noteindex.importer.annotation;

import java.lang.annotation.*;

/**
 * Marks a document importer implementation with metadata used for discovery.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ImporterPlugin {

    /**
     * Display name of the importer.
     *
     * @return importer name
     */
    String name();

    /**
     * Format identifier handled by the importer.
     *
     * @return format identifier
     */
    String formatId();

    /**
     * Supported file extensions.
     *
     * @return supported extensions
     */
    String[] extensions();
}
