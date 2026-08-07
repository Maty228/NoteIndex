package cz.martim12.noteindex.gui.settings;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiPreferencesTest {

    @Test
    void usesDefaultsForNewPreferences() throws Exception {
        Preferences store = temporaryPreferences();

        try {
            GuiPreferences preferences = new GuiPreferences(store);

            assertEquals(
                    ThemePreference.SYSTEM,
                    preferences.theme()
            );

            assertEquals(
                    50,
                    preferences.searchResultLimit()
            );

        } finally {
            store.removeNode();
        }
    }

    @Test
    void persistsThemeAndSearchResultLimit() throws Exception {
        Preferences store = temporaryPreferences();

        try {
            GuiPreferences preferences = new GuiPreferences(store);

            preferences.setTheme(ThemePreference.DARK);
            preferences.setSearchResultLimit(100);

            GuiPreferences reloaded = new GuiPreferences(store);

            assertEquals(
                    ThemePreference.DARK,
                    reloaded.theme()
            );

            assertEquals(
                    100,
                    reloaded.searchResultLimit()
            );

        } finally {
            store.removeNode();
        }
    }

    @Test
    void rejectsUnsupportedSearchResultLimit() throws Exception {
        Preferences store = temporaryPreferences();

        try {
            GuiPreferences preferences = new GuiPreferences(store);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> preferences.setSearchResultLimit(37)
            );

        } finally {
            store.removeNode();
        }
    }

    private Preferences temporaryPreferences() {
        return Preferences.userRoot().node(
                "cz/martim12/noteindex/tests/"
                        + UUID.randomUUID()
        );
    }
}