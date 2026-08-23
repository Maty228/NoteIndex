package cz.martim12.noteindex.gui.settings;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Stores and exposes user interface preferences.
 *
 * <p>Preferences are persisted using {@link Preferences} and exposed through
 * JavaFX properties for UI binding.</p>
 */
public final class GuiPreferences {

    /**
     * Default theme used when no preference has been stored.
     */
    public static final ThemePreference DEFAULT_THEME = ThemePreference.SYSTEM;
    /**
     * Default maximum number of search results displayed.
     */
    public static final int DEFAULT_SEARCH_RESULT_LIMIT = 50;

    /**
     * Supported search result limit values.
     */
    public static final List<Integer> SEARCH_RESULT_LIMITS = List.of(10, 25, 50, 100, 200);

    private static final String THEME_KEY = "theme";
    private static final String SEARCH_RESULT_LIMIT_KEY = "searchResultLimit";

    private final Preferences preferences;

    private final ReadOnlyObjectWrapper<ThemePreference> theme = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper searchResultLimit = new ReadOnlyIntegerWrapper();

    /**
     * Creates GUI preferences backed by the application preference store.
     */
    public GuiPreferences() {
        this(Preferences.userNodeForPackage(GuiPreferences.class).node("settings"));
    }

    GuiPreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "Preferences must not be null");

        theme.set(loadTheme());
        searchResultLimit.set(loadSearchResultLimit());

    }

    /**
     * Returns the currently selected theme preference.
     *
     * @return current theme
     */
    public ThemePreference theme() {
        return theme.get();
    }

    /**
     * Returns the observable theme preference property.
     *
     * @return theme property
     */
    public ReadOnlyObjectProperty<ThemePreference> themeProperty() {
        return theme.getReadOnlyProperty();
    }

    /**
     * Updates and persists the selected theme.
     *
     * @param theme new theme preference
     */
    public void setTheme(ThemePreference theme) {
        ThemePreference value = Objects.requireNonNull(theme, "Theme must not be null");

        this.theme.set(value);
        preferences.put(THEME_KEY, value.name());
    }

    /**
     * Returns the configured search result limit.
     *
     * @return maximum number of results
     */
    public int searchResultLimit() {
        return searchResultLimit.get();
    }

    /**
     * Returns the observable search result limit property.
     *
     * @return result limit property
     */
    public ReadOnlyIntegerProperty searchResultLimitProperty() {
        return searchResultLimit.getReadOnlyProperty();
    }

    /**
     * Updates and persists the search result limit.
     *
     * @param limit new result limit
     * @throws IllegalArgumentException if the limit is unsupported
     */
    public void setSearchResultLimit(int limit) {
        if (!SEARCH_RESULT_LIMITS.contains(limit)) {
            throw new IllegalArgumentException(
                    "Unsupported search result limit: " + limit
            );
        }

        searchResultLimit.set(limit);
        preferences.putInt(SEARCH_RESULT_LIMIT_KEY, limit);
    }

    private ThemePreference loadTheme() {
        String stored = preferences.get(THEME_KEY, DEFAULT_THEME.name());

        try {
            return ThemePreference.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return DEFAULT_THEME;
        }
    }

    private int loadSearchResultLimit() {
        int stored = preferences.getInt(SEARCH_RESULT_LIMIT_KEY, DEFAULT_SEARCH_RESULT_LIMIT);

        if (!SEARCH_RESULT_LIMITS.contains(stored)) {
            return DEFAULT_SEARCH_RESULT_LIMIT;
        }

        return stored;
    }

}
