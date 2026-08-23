package cz.martim12.noteindex.search.index;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identifies a searchable document field.
 * Standard fields are provided, but custom fields can be created
 * for future document structures such as headings or formulas.
 */
public record FieldName(String value) {

    private static final Pattern VALID_NAME =
            Pattern.compile("[a-z][a-z0-9_-]*");
    public static final FieldName TITLE = new FieldName("title");
    public static final FieldName BODY = new FieldName("body");



    /**
     * Creates a validated field name.
     *
     * @param value field name value
     * @throws NullPointerException if the value is null
     * @throws IllegalArgumentException if the name format is invalid
     */
    public FieldName {
        Objects.requireNonNull(value, "Field name must not be null");

        value = value.trim().toLowerCase(Locale.ROOT);

        if (!VALID_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid field name: " + value
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }

}
