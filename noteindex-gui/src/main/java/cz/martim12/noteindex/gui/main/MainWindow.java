package cz.martim12.noteindex.gui.main;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.nio.file.Path;

/**
 * Main NoteIndex workspace.
 */
public final class MainWindow {

    private final BorderPane root;
    private final VBox sidebar;
    private final SplitPane workspace;
    private final StackPane centerStack;

    private final StackPane startupOverlay;
    private final ProgressIndicator startupProgress;
    private final Label startupTitle;
    private final Label startupDescription;

    private final Label statusDot;
    private final Label statusText;

    private Button toolbarImportButton;
    private Button welcomeImportButton;

    private boolean sidebarVisible = true;

    public MainWindow() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");

        sidebar = createSidebar();

        BorderPane documentPane = createDocumentListPane();

        BorderPane viewerPane = createViewerPane();

        workspace = new SplitPane(sidebar, documentPane, viewerPane);

        workspace.setOrientation(Orientation.HORIZONTAL);

        workspace.getStyleClass().add("main-split-pane");

        workspace.setDividerPositions(0.18, 0.46);

        SplitPane.setResizableWithParent(sidebar, false);

        startupProgress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);

        startupTitle = new Label();
        startupDescription = new Label();

        startupOverlay = createStartupOverlay();

        centerStack = new StackPane(workspace, startupOverlay);

        statusDot = new Label("●");
        statusText = new Label("Not connected");

        root.setTop(createToolbar());
        root.setCenter(centerStack);
        root.setBottom(createStatusBar());

        hideStartupOverlay();
    }

    public Parent root(){
        return root;
    }

    /**
     * Displays the startup state while SQLite is opened and the
     * search index is rebuilt.
     */
    public void showStarting(Path databaseFile) {
        startupTitle.setText("Opening your library…");
        startupDescription.setText("Loading the local database and rebuilding the search index.\n" + databaseFile);

        showStartupOverlay();

        workspace.setDisable(true);

        setImportDisabled(true);

        setStatus("Opening library and rebuilding index...", "status-dot-loading");

    }

    /**
     * Marks application startup as complete.
     */
    public void showReady(Path databaseFile) {
        hideStartupOverlay();

        workspace.setDisable(false);

        setImportDisabled(false);

        setStatus("Library ready", "status-dot-ready");

        statusText.setTooltip(new Tooltip(databaseFile.toString()));
    }

    /**
     * Displays a non-interactive failure state behind the fatal
     * startup dialog.
     */
    public void showStartupFailure(Path databaseFile, Throwable failure) {
        startupProgress.setVisible(false);
        startupProgress.setManaged(false);

        startupTitle.setText("Could not open library");

        String message = displayMessage(failure);

        if (databaseFile != null) {
            startupDescription.setText(message + "\n\n" + databaseFile);
        } else {
            startupDescription.setText(message);
        }

        startupOverlay.setVisible(true);
        startupOverlay.setManaged(true);

        workspace.setDisable(true);

        setImportDisabled(true);

        setStatus("Library unavailable", "status-dot-error");
    }

    private HBox createToolbar() {
        Button sidebarButton =
                createIconButton("☰", "Toggle sidebar");

        sidebarButton.setOnAction(event -> toggleSidebar());

        Label brand = new Label("NoteIndex");
        brand.getStyleClass().add("brand-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Search your notes...");

        searchField.setEditable(false);
        searchField.setFocusTraversable(false);

        searchField.setTooltip(
                new Tooltip("Search will become available when the library is connected.")
        );
        searchField.getStyleClass().add("global-search-field");
        searchField.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(searchField, Priority.ALWAYS);

        toolbarImportButton = createImportButton("+");



        Button moreButton = createIconButton("•••", "More actions");

        HBox toolbar = new HBox(
                12, sidebarButton, brand, searchField, toolbarImportButton, moreButton
        );

        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("top-toolbar");

        return toolbar;

    }

    private VBox createSidebar() {
        Label libraryHeading = createSectionHeading("LIBRARY");

        ToggleGroup navigationGroup = new ToggleGroup();

        ToggleButton allNotes = createNavigationButton("All Notes", navigationGroup);

        ToggleButton recent = createNavigationButton("Recent", navigationGroup);

        allNotes.setSelected(true);

        Label formatsHeading = createSectionHeading("FORMATS");

        ToggleButton textNotes = createNavigationButton("TXT", navigationGroup);

        ToggleButton markdownNotes = createNavigationButton("Markdown", navigationGroup);

        Region expandingSpace = new Region();

        VBox.setVgrow(expandingSpace, Priority.ALWAYS);

        Separator separator = new Separator();

        Button settings = createSidebarAction("Settings");

        Button about = createSidebarAction("About");

        VBox sidebarPane = new VBox(
                6, libraryHeading, allNotes, recent, createSectionSpacing(), formatsHeading, textNotes, markdownNotes, expandingSpace, separator, settings, about
        );

        sidebarPane.getStyleClass().add("library-sidebar");

        sidebarPane.setMinWidth(190);
        sidebarPane.setPrefWidth(220);
        sidebarPane.setMaxWidth(260);

        return sidebarPane;
    }

    private BorderPane createDocumentListPane() {
        Label title = new Label("Documents");

        title.getStyleClass().add("pane-title");

        Label documentCount = new Label("0 notes");

        documentCount.getStyleClass().add("pane-metadata");

        Region headingSpace = new Region();

        HBox.setHgrow(headingSpace, Priority.ALWAYS);

        HBox heading = new HBox(8, title, headingSpace, documentCount);

        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getStyleClass().add("pane-heading");

        ListView<String> documentList = new ListView<>();

        documentList.getStyleClass().add("document-list");

        documentList.setPlaceholder(createListPlaceholder());

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("document-list-pane");

        pane.setTop(heading);
        pane.setCenter(documentList);

        pane.setMinWidth(260);
        pane.setPrefWidth(340);

        return pane;
    }

    private BorderPane createViewerPane() {
        Label title = new Label("Document");

        title.getStyleClass().add("pane-title");

        HBox heading = new HBox(title);

        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getStyleClass().add("pane-heading");

        BorderPane viewer = new BorderPane();

        viewer.getStyleClass().add("document-viewer-pane");

        viewer.setTop(heading);
        viewer.setCenter(createWelcomeState());

        return viewer;
    }


    private VBox createWelcomeState() {
        Label plusBadge = new Label("+");

        plusBadge.getStyleClass().add("welcome-plus");

        Label title = new Label("Your notes, instantly searchable");

        title.getStyleClass().add("welcome-title");

        Label description = new Label("Import your study notes into one local library, then find them using fast ranked search.");

        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);

        description.setMaxWidth(460);

        description.getStyleClass().add("welcome-description");

        welcomeImportButton = createImportButton("Import notes");

        welcomeImportButton.getStyleClass().remove("import-button");

        welcomeImportButton.getStyleClass().add("primary-button");

        Label dropHint = new Label("You will also be able to drop files anywhere in this window.");

        dropHint.setWrapText(true);
        dropHint.setTextAlignment(TextAlignment.CENTER);

        dropHint.getStyleClass().add("welcome-hint");

        Label formats = new Label("TXT  ·  MD");

        formats.getStyleClass().add("supported-formats");

        VBox welcome = new VBox(
                14, plusBadge, title, description, welcomeImportButton, dropHint, formats
        );

        welcome.setAlignment(Pos.CENTER);
        welcome.setPadding(new Insets(40));

        welcome.getStyleClass().add("welcome-state");

        return welcome;
    }

    private StackPane createStartupOverlay() {
        startupProgress.setMaxSize(42, 42);

        startupTitle.getStyleClass().add("startup-title");

        startupDescription.setWrapText(true);

        startupDescription.setTextAlignment(TextAlignment.CENTER);

        startupDescription.setMaxWidth(480);

        startupDescription.getStyleClass().add("startup-description");

        VBox card = new VBox(16, startupProgress, startupTitle, startupDescription);

        card.setAlignment(Pos.CENTER);

        card.setPadding(new Insets(32, 40, 32, 40));

        card.getStyleClass().add("startup-card");

        StackPane overlay = new StackPane(card);

        overlay.setAlignment(Pos.CENTER);

        overlay.getStyleClass().add("startup-overlay");

        return overlay;
    }

    private VBox createListPlaceholder() {
        Label symbol = new Label("≡");

        symbol.getStyleClass().add("list-placeholder-symbol");

        Label title = new Label("No notes yet");

        title.getStyleClass().add("list-placeholder-title");

        Label description = new Label("Imported notes will appear here.");

        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);

        description.getStyleClass().add("list-placeholder-description");

        VBox placeholder = new VBox(8, symbol, title, description);

        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(24));

        return placeholder;
    }

    private HBox createStatusBar() {
        Label documentCount = new Label("0 documents");

        documentCount.getStyleClass().add("status-text");

        Region spacing = new Region();

        HBox.setHgrow(spacing, Priority.ALWAYS);

        statusDot.getStyleClass().add("status-dot");

        statusText.getStyleClass().add("status-text");

        HBox statusBar = new HBox(7, documentCount, spacing, statusDot, statusText);

        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusBar.getStyleClass().add("status-bar");

        return statusBar;
    }

    private Button createImportButton(String text) {
        Button button = new Button(text);

        button.getStyleClass().add("import-button");

        button.setTooltip(new Tooltip("Import notes"));

        button.setAccessibleText("Import notes");

        button.setOnAction(event -> showImportPlaceholder());

        return button;
    }

    private Button createIconButton(String text, String accessibleText) {
        Button button = new Button(text);

        button.getStyleClass().add("toolbar-icon-button");

        button.setTooltip(new Tooltip(accessibleText));

        button.setAccessibleText(accessibleText);

        return button;
    }

    private Label createSectionHeading(String text) {
        Label heading = new Label(text);

        heading.getStyleClass().add("sidebar-section-heading");

        return heading;
    }

    private ToggleButton createNavigationButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);

        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);

        button.getStyleClass().add("sidebar-navigation-button");

        return button;
    }

    private Button createSidebarAction(String text) {
        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);

        button.getStyleClass().add("sidebar-action-button");

        return button;
    }

    private Region createSectionSpacing() {
        Region spacing = new Region();

        spacing.setMinHeight(12);

        return spacing;
    }

    private void toggleSidebar() {
        if (sidebarVisible) {
            workspace.getItems().remove(sidebar);

            workspace.setDividerPositions(0.34);
        } else {
            workspace.getItems().add(0, sidebar);

            workspace.setDividerPositions(0.18, 0.46);
        }

        sidebarVisible = !sidebarVisible;
    }

    private void setImportDisabled(boolean disabled) {
        if (toolbarImportButton != null) {
            toolbarImportButton.setDisable(disabled);
        }

        if (welcomeImportButton != null) {
            welcomeImportButton.setDisable(disabled);
        }
    }

    private void showStartupOverlay() {
        startupOverlay.setVisible(true);
        startupOverlay.setManaged(true);

        startupProgress.setVisible(true);
        startupProgress.setManaged(true);
    }

    private void hideStartupOverlay() {
        startupOverlay.setVisible(false);
        startupOverlay.setManaged(false);

        startupProgress.setVisible(false);
        startupProgress.setManaged(false);
    }

    private void setStatus(String text, String dotStyle) {
        statusText.setText(text);

        statusDot.getStyleClass(). removeAll("status-dot-loading", "status-dot-ready", "status-dot-error");

        statusDot.getStyleClass().add(dotStyle);
    }

    private void showImportPlaceholder() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Import notes");

        alert.setHeaderText("The NoteIndex library is ready.");

        alert.setContentText("File selection and full-window drag and drop will be connected in the import workflow.");

        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }

        alert.showAndWait();
    }

    private static String displayMessage(Throwable failure) {
        if (failure == null) {
            return "Unknown startup failure";
        }
        Throwable current = failure;

        while (current != null) {
            String message = current.getMessage();

            if (message != null && !message.isBlank()) {
                return message;
            }

            if (current.getCause() == current) {
                break;
            }

            current = current.getCause();
        }

        return failure.getClass().getSimpleName();
    }


}
