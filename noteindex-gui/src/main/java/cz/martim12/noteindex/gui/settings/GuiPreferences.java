package cz.martim12.noteindex.gui.settings;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
public final class GuiPreferences {

    public static final ThemePreference DEFAULT_THEME = ThemePreference.SYSTEM;
    public static final int DEFAULT_SEARCH_RESULT_LIMIT = 50;

    public static final List<Integer> SEARCH_RESULT_LIMITS = List.of(10, 25, 50, 100, 200);

    private static final String THEME_KEY = "theme";
    private static final String SEARCH_RESULT_LIMIT_KEY = "searchResultLimit";

    private final Preferences preferences;

    private final ReadOnlyObjectWrapper<ThemePreference> theme = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper searchResultLimit = new ReadOnlyIntegerWrapper();

    public GuiPreferences() {
        this(Preferences.userNodeForPackage(GuiPreferences.class).node("settings"));
    }

    GuiPreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "Preferences must not be null");

        theme.set(loadTheme());
        searchResultLimit.set(loadSearchResultLimit());

    }

    public ThemePreference theme() {
        return theme.get();
    }

    public ReadOnlyObjectProperty<ThemePreference> themeProperty() {
        return theme.getReadOnlyProperty();
    }

    public void setTheme(ThemePreference theme) {
        ThemePreference value = Objects.requireNonNull(theme, "Theme must not be null");

        this.theme.set(value);
        preferences.put(THEME_KEY, value.name());
    }

    public int searchResultLimit() {
        return searchResultLimit.get();
    }

    public ReadOnlyIntegerProperty searchResultLimitProperty() {
        return searchResultLimit.getReadOnlyProperty();
    }

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
