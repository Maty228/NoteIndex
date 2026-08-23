package cz.martim12.noteindex.gui.settings;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;

import java.util.Objects;

/**
 * Manages application theme stylesheets and responds to theme changes.
 *
 * <p>The manager supports explicit light/dark themes as well as following
 * the operating system color scheme.</p>
 */
public final class ThemeManager implements AutoCloseable {
    private final Scene scene;
    private final GuiPreferences preferences;

    private final String lightStylesheet;
    private final String darkStylesheet;

    private final ChangeListener<ThemePreference> themeListener;
    private final ChangeListener<ColorScheme> platformThemeListener;

    /**
     * Creates a theme manager.
     *
     * @param scene scene whose stylesheets are managed
     * @param preferences GUI preferences containing theme selection
     * @param lightStylesheet light theme stylesheet
     * @param darkStylesheet dark theme stylesheet
     */
    public ThemeManager(Scene scene, GuiPreferences preferences, String lightStylesheet, String darkStylesheet) {
        this.scene = Objects.requireNonNull(scene, "Scene must not be null");
        this.preferences = Objects.requireNonNull(preferences, "Preferences must not be null");
        this.lightStylesheet = Objects.requireNonNull(lightStylesheet, "Light stylesheet must not be null");
        this.darkStylesheet = Objects.requireNonNull(darkStylesheet, "Dark stylesheet must not be null");

        themeListener = (observable, oldValue, newValue) -> applyTheme();
        platformThemeListener = (observable, oldValue, newValue) -> {
            if (preferences.theme() == ThemePreference.SYSTEM) {
                applyTheme();
            }
        };

    }

    /**
     * Starts listening for theme preference changes and applies the current theme.
     */
    public void start() {
        preferences.themeProperty().addListener(themeListener);

        Platform.getPreferences()
                .colorSchemeProperty()
                .addListener(platformThemeListener);

        applyTheme();
    }

    /**
     * Stops listening for theme changes and releases resources.
     */
    @Override
    public void close() {
        preferences.themeProperty().removeListener(themeListener);
        Platform.getPreferences()
                .colorSchemeProperty()
                .removeListener(platformThemeListener);
    }

    private void applyTheme() {
        String stylesheet = resolveStylesheet();

        scene.getStylesheets().removeAll(lightStylesheet, darkStylesheet);

        scene.getStylesheets().add(stylesheet);
    }

    private String resolveStylesheet() {
        return switch (preferences.theme()) {
            case LIGHT -> lightStylesheet;
            case DARK -> darkStylesheet;
            case SYSTEM -> Platform.getPreferences().getColorScheme() == ColorScheme.DARK ? darkStylesheet : lightStylesheet;
        };
    }
}
