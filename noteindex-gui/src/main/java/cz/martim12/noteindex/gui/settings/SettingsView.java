package cz.martim12.noteindex.gui.settings;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.Objects;

/**
 * JavaFX view displaying application settings and information.
 */
public final class SettingsView {

    private final BorderPane root = new BorderPane();
    private final ScrollPane scrollPane = new ScrollPane();

    private Runnable closeAction = () -> {};

    /**
     * Creates a settings view.
     *
     * @param preferences GUI preference storage
     * @param databaseFile current database location
     * @param documentCount observable document count
     */
    public SettingsView(GuiPreferences preferences, Path databaseFile, ReadOnlyIntegerProperty documentCount) {

        Objects.requireNonNull(preferences, "Preferences must not be null");
        Objects.requireNonNull(databaseFile, "Database file must not be null");
        Objects.requireNonNull(documentCount, "Document count must not be null");

        root.getStyleClass().add("settings-view");

        root.setTop(createHeader());

        VBox content = new VBox(
                28, createAppearanceSection(preferences), createSearchSection(preferences), createLibrarySection(databaseFile, documentCount), createAboutSection()
        );

        content.setMaxWidth(760);
        content.setPadding(new Insets(32, 40, 48, 40));
        content.getStyleClass().add("settings-content");

        VBox centeredContent = new VBox(content);
        centeredContent.setAlignment(Pos.TOP_CENTER);

        scrollPane.setContent(centeredContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("settings-scroll-pane");

        root.setCenter(scrollPane);
    }

    /**
     * Returns the root node of this settings view.
     *
     * @return settings view root
     */
    public Parent root() {
        return root;
    }

    /**
     * Sets the action executed when leaving the settings view.
     *
     * @param closeAction action invoked when closing settings
     */
    public void setOnClose(Runnable closeAction) {
        this.closeAction = Objects.requireNonNull(closeAction, "Close action must not be null");
    }

    /**
     * Scrolls the settings view to the top.
     */
    public void showTop() {
        Platform.runLater(() -> scrollPane.setVvalue(0));
    }

    /**
     * Scrolls the settings view to the about section.
     */
    public void showAbout() {
        Platform.runLater(() -> scrollPane.setVvalue(1));
    }

    private HBox createHeader() {
        Button backButton = new Button("←  Back");
        backButton.getStyleClass().add("settings-back-button");
        backButton.setAccessibleText("Back to library");
        backButton.setOnAction(event -> closeAction.run());

        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");

        HBox header = new HBox(12, backButton, title);

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 20, 0, 20));
        header.getStyleClass().add("settings-header");

        return header;
    }

    private VBox createAppearanceSection(GuiPreferences preferences) {
        ComboBox<ThemePreference> themeBox = new ComboBox<>();

        themeBox.getItems().setAll(ThemePreference.values());
        themeBox.setValue(preferences.theme());
        themeBox.setPrefWidth(150);

        themeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemePreference theme) {
                if (theme == null) {
                    return "";
                }

                return switch (theme) {
                    case SYSTEM -> "System";
                    case LIGHT -> "Light";
                    case DARK -> "Dark";
                };
            }

            @Override
            public ThemePreference fromString(String value) {
                throw new UnsupportedOperationException();
            }
        });

        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                preferences.setTheme(newValue);
            }
        });

        return createSection(
                "Appearance",
                createSettingRow(
                        "Theme",
                        "Use the system appearance or choose a fixed theme.",
                        themeBox
                )
        );
    }

    private VBox createSearchSection(GuiPreferences preferences) {
        ComboBox<Integer> resultLimitBox = new ComboBox<>();

        resultLimitBox.getItems().setAll(
                GuiPreferences.SEARCH_RESULT_LIMITS
        );

        resultLimitBox.setValue(
                preferences.searchResultLimit()
        );

        resultLimitBox.setPrefWidth(110);

        resultLimitBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        preferences.setSearchResultLimit(newValue);
                    }
                }
        );

        return createSection(
                "Search",
                createSettingRow(
                        "Result limit",
                        "Maximum number of ranked results returned for one search.",
                        resultLimitBox
                )
        );
    }

    private VBox createLibrarySection(
            Path databaseFile,
            ReadOnlyIntegerProperty documentCount
    ) {
        Label path = new Label(databaseFile.toString());

        path.setWrapText(true);
        path.setMaxWidth(420);
        path.getStyleClass().add("settings-value");

        Label count = new Label();
        count.textProperty().bind(
                documentCount.asString("%d documents")
        );

        count.getStyleClass().add("settings-value");

        Label indexStatus = new Label("Ready");
        indexStatus.getStyleClass().addAll(
                "settings-value",
                "settings-ready-value"
        );

        return createSection(
                "Library",
                createSettingRow(
                        "Database",
                        "Local SQLite database used by NoteIndex.",
                        path
                ),
                createSettingRow(
                        "Documents",
                        "Number of documents currently stored in the library.",
                        count
                ),
                createSettingRow(
                        "Search index",
                        "The in-memory index is rebuilt when NoteIndex starts.",
                        indexStatus
                )
        );
    }

    private VBox createAboutSection() {
        Label version = new Label(applicationVersion());
        version.getStyleClass().add("settings-value");

        Label javaVersion = new Label(
                System.getProperty("java.version", "Unknown")
        );

        javaVersion.getStyleClass().add("settings-value");

        Label javafxVersion = new Label(
                System.getProperty("javafx.version", "Unknown")
        );

        javafxVersion.getStyleClass().add("settings-value");

        Label description = new Label(
                "A local-first desktop application for importing, "
                        + "browsing and searching study notes."
        );

        description.setWrapText(true);
        description.setMaxWidth(420);
        description.getStyleClass().add("settings-description");

        return createSection(
                "About",
                description,
                createSettingRow(
                        "Version",
                        null,
                        version
                ),
                createSettingRow(
                        "Java",
                        null,
                        javaVersion
                ),
                createSettingRow(
                        "JavaFX",
                        null,
                        javafxVersion
                )
        );
    }

    private VBox createSection(String title, Node... children) {
        Label heading = new Label(title);
        heading.getStyleClass().add("settings-section-title");

        VBox section = new VBox(12);
        section.getChildren().add(heading);
        section.getChildren().addAll(children);

        section.getStyleClass().add("settings-section");

        return section;
    }

    private HBox createSettingRow(
            String titleText,
            String descriptionText,
            Node control
    ) {
        Label title = new Label(titleText);
        title.getStyleClass().add("settings-row-title");

        VBox text;

        if (descriptionText == null || descriptionText.isBlank()) {
            text = new VBox(title);
        } else {
            Label description = new Label(descriptionText);

            description.setWrapText(true);
            description.setMaxWidth(440);
            description.getStyleClass().add("settings-row-description");

            text = new VBox(4, title, description);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(18, text, spacer, control);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.getStyleClass().add("settings-row");

        return row;
    }

    private static String applicationVersion() {
        return SettingsView.class.getModule()
                .getDescriptor()
                .version()
                .map(Object::toString)
                .orElse("Development");
    }
}
