package cz.martim12.noteindex.importer.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ImporterPlugin {
    String name();
    String formatId();
    String[] extensions();
}
