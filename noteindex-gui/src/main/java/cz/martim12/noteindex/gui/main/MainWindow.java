package cz.martim12.noteindex.gui.main;

import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.gui.importflow.ImportCoordinator;
import cz.martim12.noteindex.gui.library.DocumentListCell;
import cz.martim12.noteindex.gui.viewer.DocumentViewer;
import cz.martim12.noteindex.gui.importflow.ImportBatchResult;
import cz.martim12.noteindex.gui.importflow.ImportProgressDialog;
import cz.martim12.noteindex.gui.importflow.ImportFileSupport;
import cz.martim12.noteindex.core.model.SearchResult;
import cz.martim12.noteindex.gui.search.SearchCoordinator;
import cz.martim12.noteindex.gui.search.SearchResultCell;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
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
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

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

    private ImportCoordinator importCoordinator;
    private boolean importInProgress;

    private final StackPane windowStack;
    private final StackPane dropOverlay;

    private Label dropOverlaySymbol;
    private Label dropOverlayTitle;
    private Label dropOverlayDescription;
    private Label dropOverlayFormats;

    private ImportFileSupport importFileSupport;

    private ToggleButton allNotesButton;
    private ToggleButton recentButton;
    private ToggleButton textNotesButton;
    private ToggleButton markdownNotesButton;

    private TextField searchField;
    private Label documentPaneTitle;
    private ListView<SearchResult> searchResultList;
    private StackPane documentListsStack;
    private SearchCoordinator searchCoordinator;
    private boolean searchMode;

    private ComboBox<MainViewModel.DocumentSort> sortBox;
    private ListView<DocumentSummary> documentList;

    private Label documentCountLabel;
    private Label statusDocumentCount;

    private final DocumentViewer documentViewer;
    private final Node welcomeState;
    private final StackPane viewerStack;

    private MainViewModel viewModel;

    private boolean sidebarVisible = true;

    public MainWindow() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");

        sidebar = createSidebar();

        BorderPane documentPane = createDocumentListPane();

        documentViewer = new DocumentViewer();
        welcomeState = createWelcomeState();

        viewerStack = new StackPane(welcomeState, documentViewer.root());

        documentViewer.root().setVisible(false);
        documentViewer.root().setManaged(false);

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

        dropOverlay = createDropOverlay();

        windowStack = new StackPane(root, dropOverlay);

        configureDragAndDrop();

        hideStartupOverlay();
        hideDropOverlay();
    }

    public Parent root(){
        return windowStack;
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

    public void connect(MainViewModel viewModel) {
        this.viewModel = viewModel;

        documentList.setItems(viewModel.visibleDocuments());

        allNotesButton.setOnAction(event ->
                activateLibraryView(MainViewModel.LibraryView.ALL)
        );

        recentButton.setOnAction(event ->
                activateLibraryView(MainViewModel.LibraryView.RECENT)
        );

        textNotesButton.setOnAction(event ->
                activateLibraryView(MainViewModel.LibraryView.TXT)
        );

        markdownNotesButton.setOnAction(event ->
                activateLibraryView(MainViewModel.LibraryView.MARKDOWN)
        );

        recentButton.setOnAction(event ->
                viewModel.setLibraryView(MainViewModel.LibraryView.RECENT)
        );

        textNotesButton.setOnAction(event ->
                viewModel.setLibraryView(MainViewModel.LibraryView.TXT)
        );

        markdownNotesButton.setOnAction(event ->
                viewModel.setLibraryView(MainViewModel.LibraryView.MARKDOWN)
        );

        sortBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.setDocumentSort(newValue);
            }
        });

        documentList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.selectDocument(newValue)
        );

        viewModel.selectedDocumentProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue == null) {
                        documentViewer.showEmpty();
                    } else {
                        documentViewer.showDocument(newValue);
                    }
                }
        );

        viewModel.totalDocumentCountProperty().addListener(
                (observable, oldValue, newValue) -> updateDocumentCount(newValue.intValue())
        );

        viewModel.libraryLoadingProperty().addListener(
                (observable, oldValue, loading) -> {
                    if (loading) {
                        setStatus("Loading documents...", "status-dot-loading");
                    } else {
                        setStatus("Library ready", "status-dot-ready");
                    }
                }
        );

        viewModel.errorProperty().addListener(
                (observable, oldValue, failure) -> {
                    if (failure != null) {
                        showOperationError(failure);
                    }
                }
        );

        documentList.getItems().addListener((ListChangeListener<DocumentSummary>) change -> {
            if (!documentList.getItems().isEmpty()
                    && documentList.getSelectionModel().getSelectedItem() == null) {

                documentList.getSelectionModel().selectFirst();
            }
        });

        updateDocumentCount(viewModel.totalDocumentCountProperty().get());
    }

    private void activateLibraryView(MainViewModel.LibraryView libraryView) {
        if (searchField != null && !searchField.getText().isBlank()) {
            searchField.clear();
        }

        viewModel.setLibraryView(libraryView);
    }

    private void updateDocumentCount(int count) {
        documentCountLabel.setText(count + (count == 1 ? " note" : " notes"));
        statusDocumentCount.setText(count + (count == 1 ? " document" : " documents"));

        boolean empty = count == 0;

        welcomeState.setVisible(empty);
        welcomeState.setManaged(empty);

        documentViewer.root().setVisible(!empty);
        documentViewer.root().setManaged(!empty);

        if (empty) {
            documentViewer.showEmpty();
        }
    }

    public void connectImport(ImportCoordinator importCoordinator) {
        this.importCoordinator = importCoordinator;

        importFileSupport = new ImportFileSupport(
                importCoordinator.supportedExtensions()
        );

        dropOverlayFormats.setText(importFileSupport.supportedFormatsLabel());
    }

    private StackPane createDropOverlay() {
        dropOverlaySymbol = new Label("+");
        dropOverlaySymbol.getStyleClass().add("drop-overlay-symbol");

        dropOverlayTitle = new Label("Drop notes to import");
        dropOverlayTitle.getStyleClass().add("drop-overlay-title");

        dropOverlayDescription = new Label("Release anywhere in this window");
        dropOverlayDescription.getStyleClass().add("drop-overlay-description");

        dropOverlayFormats = new Label("TXT · MD · MARKDOWN");
        dropOverlayFormats.getStyleClass().add("drop-overlay-formats");

        VBox card = new VBox(
                12,
                dropOverlaySymbol,
                dropOverlayTitle,
                dropOverlayDescription,
                dropOverlayFormats
        );

        card.setAlignment(Pos.CENTER);
        card.setMaxSize(520, 300);
        card.setPadding(new Insets(42));

        card.getStyleClass().add("drop-overlay-card");

        StackPane overlay = new StackPane(card);

        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(true);

        overlay.getStyleClass().addAll(
                "drop-overlay",
                "drop-overlay-accepted"
        );

        return overlay;
    }

    private void configureDragAndDrop() {
        windowStack.setOnDragOver(this::handleDragOver);
        windowStack.setOnDragDropped(this::handleDragDropped);

        windowStack.setOnDragExited(event -> {
            if (event.getTarget() == windowStack) {
                hideDropOverlay();
            }
        });
    }

    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (!dragboard.hasFiles()
                || importCoordinator == null
                || importFileSupport == null
                || importInProgress) {

            hideDropOverlay();
            return;
        }

        List<Path> paths = dragboard.getFiles().stream()
                .map(File::toPath)
                .toList();

        if (importFileSupport.containsSupportedFile(paths)) {
            event.acceptTransferModes(TransferMode.COPY);
            showAcceptedDropOverlay();
        } else {
            showRejectedDropOverlay();
        }

        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        boolean completed = false;

        if (dragboard.hasFiles()
                && importCoordinator != null
                && importFileSupport != null
                && !importInProgress) {

            List<Path> paths = dragboard.getFiles().stream()
                    .map(File::toPath)
                    .toList();

            if (importFileSupport.containsSupportedFile(paths)) {
                List<Path> files = importFileSupport.regularFiles(paths);

                if (!files.isEmpty()) {
                    hideDropOverlay();
                    importFiles(files);

                    completed = true;
                }
            }
        }

        hideDropOverlay();

        event.setDropCompleted(completed);
        event.consume();
    }

    private void showAcceptedDropOverlay() {
        dropOverlaySymbol.setText("+");
        dropOverlayTitle.setText("Drop notes to import");
        dropOverlayDescription.setText("Release anywhere in this window");

        dropOverlay.getStyleClass().removeAll(
                "drop-overlay-accepted",
                "drop-overlay-rejected"
        );

        dropOverlay.getStyleClass().add("drop-overlay-accepted");

        showDropOverlay();
    }

    private void showRejectedDropOverlay() {
        dropOverlaySymbol.setText("×");
        dropOverlayTitle.setText("Unsupported files");
        dropOverlayDescription.setText("NoteIndex cannot import these files yet");

        dropOverlay.getStyleClass().removeAll(
                "drop-overlay-accepted",
                "drop-overlay-rejected"
        );

        dropOverlay.getStyleClass().add("drop-overlay-rejected");

        showDropOverlay();
    }

    private void showDropOverlay() {
        dropOverlay.setVisible(true);
        dropOverlay.setManaged(true);
    }

    private void hideDropOverlay() {
        dropOverlay.setVisible(false);
        dropOverlay.setManaged(false);
    }

    public void connectSearch(SearchCoordinator searchCoordinator) {
        this.searchCoordinator = searchCoordinator;

        searchResultList.setItems(searchCoordinator.results());

        searchField.setEditable(true);
        searchField.setFocusTraversable(true);

        searchField.setTooltip(
                new Tooltip("Search notes · ⌘K on macOS, Ctrl+K elsewhere")
        );

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                searchCoordinator.clear();
                showLibraryMode();
                return;
            }

            showSearchMode();
            searchCoordinator.search(newValue);
        });

        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !searchField.getText().isEmpty()) {
                searchField.clear();
                event.consume();
            }
        });

        searchResultList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        viewModel.selectDocument(newValue.document());
                    }
                }
        );

        searchCoordinator.results().addListener(
                (ListChangeListener<SearchResult>) change -> handleSearchResultsChanged()
        );

        searchCoordinator.searchingProperty().addListener(
                (observable, oldValue, searching) -> {
                    if (!searchMode) {
                        return;
                    }

                    if (searching) {
                        documentCountLabel.setText("Searching...");
                        setStatus("Searching...", "status-dot-loading");
                        return;
                    }

                    if (searchCoordinator.errorProperty().get() == null) {
                        updateSearchResultCount();

                        setStatus(
                                searchResultList.getItems().size() + " search results",
                                "status-dot-ready"
                        );
                    }
                }
        );

        searchCoordinator.errorProperty().addListener(
                (observable, oldValue, failure) -> {
                    if (failure == null) {
                        return;
                    }

                    setStatus("Search failed", "status-dot-error");
                    showOperationError(failure);
                }
        );
    }

    private void showSearchMode() {
        if (searchMode) {
            documentCountLabel.setText("Searching...");
            return;
        }

        searchMode = true;

        documentPaneTitle.setText("Search results");

        documentList.setVisible(false);
        documentList.setManaged(false);

        searchResultList.setVisible(true);
        searchResultList.setManaged(true);

        sortBox.setDisable(true);

        documentCountLabel.setText("Searching...");
    }

    private void showLibraryMode() {
        if (!searchMode) {
            return;
        }

        searchMode = false;

        documentPaneTitle.setText("Documents");

        searchResultList.setVisible(false);
        searchResultList.setManaged(false);

        documentList.setVisible(true);
        documentList.setManaged(true);

        sortBox.setDisable(false);

        updateDocumentCount(
                viewModel.totalDocumentCountProperty().get()
        );

        synchronizeLibrarySelection();

        setStatus("Library ready", "status-dot-ready");
    }

    private void handleSearchResultsChanged() {
        if (!searchMode) {
            return;
        }

        updateSearchResultCount();

        if (searchResultList.getItems().isEmpty()) {
            viewModel.selectDocument(null);
            return;
        }

        searchResultList.getSelectionModel().selectFirst();
    }

    private void updateSearchResultCount() {
        int count = searchResultList.getItems().size();

        if (count >= 50) {
            documentCountLabel.setText("50+ results");
            return;
        }

        documentCountLabel.setText(
                count + (count == 1 ? " result" : " results")
        );
    }

    private void synchronizeLibrarySelection() {
        if (viewModel.selectedDocumentProperty().get() == null) {
            if (!documentList.getItems().isEmpty()) {
                documentList.getSelectionModel().selectFirst();
            }

            return;
        }

        long selectedId = viewModel.selectedDocumentProperty().get().id();

        documentList.getItems().stream()
                .filter(document -> document.id() == selectedId)
                .findFirst()
                .ifPresentOrElse(
                        document -> documentList.getSelectionModel().select(document),
                        () -> {
                            if (!documentList.getItems().isEmpty()) {
                                documentList.getSelectionModel().selectFirst();
                            }
                        }
                );
    }

    private void showOperationError(Throwable failure) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("NoteIndex");
        alert.setHeaderText("The operation could not be completed.");
        alert.setContentText(displayMessage(failure));

        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }

        alert.showAndWait();
    }

    private HBox createToolbar() {
        Button sidebarButton =
                createIconButton("☰", "Toggle sidebar");

        sidebarButton.setOnAction(event -> toggleSidebar());

        Label brand = new Label("NoteIndex");
        brand.getStyleClass().add("brand-label");

        searchField = new TextField();

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

        allNotesButton = createNavigationButton("All Notes", navigationGroup);
        recentButton = createNavigationButton("Recent", navigationGroup);

        allNotesButton.setSelected(true);

        Label formatsHeading = createSectionHeading("FORMATS");

        textNotesButton = createNavigationButton("TXT", navigationGroup);
        markdownNotesButton = createNavigationButton("Markdown", navigationGroup);

        Region expandingSpace = new Region();
        VBox.setVgrow(expandingSpace, Priority.ALWAYS);

        Separator separator = new Separator();

        Button settings = createSidebarAction("Settings");
        Button about = createSidebarAction("About");

        VBox sidebarPane = new VBox(
                6,
                libraryHeading,
                allNotesButton,
                recentButton,
                createSectionSpacing(),
                formatsHeading,
                textNotesButton,
                markdownNotesButton,
                expandingSpace,
                separator,
                settings,
                about
        );

        sidebarPane.getStyleClass().add("library-sidebar");

        sidebarPane.setMinWidth(190);
        sidebarPane.setPrefWidth(220);
        sidebarPane.setMaxWidth(260);

        return sidebarPane;
    }

    private BorderPane createDocumentListPane() {
        documentPaneTitle = new Label("Documents");
        documentPaneTitle.getStyleClass().add("pane-title");

        documentCountLabel = new Label("0 notes");
        documentCountLabel.getStyleClass().add("pane-metadata");

        sortBox = new ComboBox<>();

        sortBox.getItems().setAll(MainViewModel.DocumentSort.values());
        sortBox.setValue(MainViewModel.DocumentSort.NEWEST);
        sortBox.setPrefWidth(125);
        sortBox.getStyleClass().add("document-sort-box");

        sortBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(MainViewModel.DocumentSort sort) {
                if (sort == null) {
                    return "";
                }

                return switch (sort) {
                    case NEWEST -> "Newest";
                    case OLDEST -> "Oldest";
                    case TITLE_ASCENDING -> "Title A–Z";
                    case TITLE_DESCENDING -> "Title Z–A";
                };
            }

            @Override
            public MainViewModel.DocumentSort fromString(String value) {
                throw new UnsupportedOperationException();
            }
        });

        Region headingSpace = new Region();
        HBox.setHgrow(headingSpace, Priority.ALWAYS);

        HBox heading = new HBox(
                8,
                documentPaneTitle,
                headingSpace,
                documentCountLabel,
                sortBox
        );

        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getStyleClass().add("pane-heading");

        documentList = new ListView<>();
        documentList.getStyleClass().add("document-list");
        documentList.setCellFactory(list -> new DocumentListCell());
        documentList.setPlaceholder(createListPlaceholder());

        searchResultList = new ListView<>();
        searchResultList.getStyleClass().addAll("document-list", "search-result-list");
        searchResultList.setCellFactory(list -> new SearchResultCell());
        searchResultList.setPlaceholder(createSearchPlaceholder());

        searchResultList.setVisible(false);
        searchResultList.setManaged(false);

        documentListsStack = new StackPane(documentList, searchResultList);

        BorderPane pane = new BorderPane();

        pane.getStyleClass().add("document-list-pane");
        pane.setTop(heading);
        pane.setCenter(documentListsStack);

        pane.setMinWidth(280);
        pane.setPrefWidth(360);

        return pane;
    }

    private VBox createSearchPlaceholder() {
        Label symbol = new Label("⌕");
        symbol.getStyleClass().add("list-placeholder-symbol");

        Label title = new Label("No matching notes");
        title.getStyleClass().add("list-placeholder-title");

        Label description = new Label(
                "Try different words or a quoted phrase."
        );

        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);
        description.getStyleClass().add("list-placeholder-description");

        VBox placeholder = new VBox(8, symbol, title, description);

        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(24));

        return placeholder;
    }

    private BorderPane createViewerPane() {
        BorderPane viewer = new BorderPane();

        viewer.getStyleClass().add("document-viewer-pane");
        viewer.setCenter(viewerStack);

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

        Label dropHint = new Label("Drop TXT or Markdown files anywhere in this window.");

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
        statusDocumentCount = new Label("0 documents");
        statusDocumentCount.getStyleClass().add("status-text");

        Region spacing = new Region();

        HBox.setHgrow(spacing, Priority.ALWAYS);

        statusDot.getStyleClass().add("status-dot");

        statusText.getStyleClass().add("status-text");

        HBox statusBar = new HBox(7, statusDocumentCount, spacing, statusDot, statusText);

        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusBar.getStyleClass().add("status-bar");

        return statusBar;
    }

    private Button createImportButton(String text) {
        Button button = new Button(text);

        button.getStyleClass().add("import-button");

        button.setTooltip(new Tooltip("Import notes"));

        button.setAccessibleText("Import notes");

        button.setOnAction(event -> handleImportRequest());

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
            workspace.getItems().addFirst(sidebar);

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
    private void handleImportRequest() {
        if (importCoordinator == null) {
            showImportPlaceholder();
            return;
        }

        if (root.getScene() == null || importInProgress) {
            return;
        }

        FileChooser chooser = createImportFileChooser();

        List<File> selectedFiles = chooser.showOpenMultipleDialog(
                root.getScene().getWindow()
        );

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        List<Path> sources = selectedFiles.stream()
                .map(File::toPath)
                .toList();

        importFiles(sources);
    }

    private FileChooser createImportFileChooser() {
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Import notes");

        Set<String> extensions = importCoordinator.supportedExtensions();

        List<String> supportedPatterns = extensions.stream()
                .sorted()
                .map(extension -> "*." + extension)
                .toList();

        if (!supportedPatterns.isEmpty()) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Supported notes",
                            supportedPatterns.toArray(String[]::new)
                    )
            );
        }

        if (extensions.contains("txt")) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Plain text",
                            "*.txt"
                    )
            );
        }

        List<String> markdownPatterns = extensions.stream()
                .filter(extension -> extension.equals("md") || extension.equals("markdown"))
                .sorted()
                .map(extension -> "*." + extension)
                .toList();

        if (!markdownPatterns.isEmpty()) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Markdown",
                            markdownPatterns.toArray(String[]::new)
                    )
            );
        }

        return chooser;
    }

    private void importFiles(List<Path> sources) {
        importInProgress = true;
        setImportDisabled(true);

        ImportProgressDialog progressDialog = new ImportProgressDialog(
                root.getScene().getWindow(),
                sources.size()
        );

        progressDialog.show();

        importCoordinator.importFiles(sources, progressDialog::update)
                .whenComplete((result, failure) ->
                        Platform.runLater(() -> {
                            if (failure != null) {
                                importInProgress = false;
                                setImportDisabled(false);

                                showOperationError(unwrapFailure(failure));
                                return;
                            }

                            progressDialog.showResult(result);
                            finishImport(result);
                        })
                );
    }

    private void finishImport(ImportBatchResult result) {
        if (result.importedDocuments().isEmpty()) {
            importInProgress = false;
            setImportDisabled(false);

            setStatus("No notes imported", "status-dot-error");
            return;
        }

        long lastImportedId = result.importedDocuments().getLast().id();

        allNotesButton.setSelected(true);
        viewModel.setLibraryView(MainViewModel.LibraryView.ALL);

        viewModel.refresh().whenComplete((ignored, failure) ->
                Platform.runLater(() -> {
                    importInProgress = false;
                    setImportDisabled(false);

                    if (failure != null) {
                        return;
                    }

                    selectDocument(lastImportedId);

                    int importedCount = result.importedDocuments().size();

                    setStatus(
                            "Imported " + importedCount + (importedCount == 1 ? " note" : " notes"),
                            "status-dot-ready"
                    );
                })
        );
    }

    private void selectDocument(long documentId) {
        documentList.getItems().stream()
                .filter(document -> document.id() == documentId)
                .findFirst()
                .ifPresent(document ->
                        documentList.getSelectionModel().select(document)
                );
    }

    public void installShortcuts(Scene scene) {
        KeyCodeCombination focusSearch = new KeyCodeCombination(
                KeyCode.K,
                KeyCombination.SHORTCUT_DOWN
        );

        scene.getAccelerators().put(focusSearch, () -> {
            if (!searchField.isEditable()) {
                return;
            }

            searchField.requestFocus();
            searchField.selectAll();
        });
    }

    private static Throwable unwrapFailure(Throwable failure) {
        Throwable current = failure;

        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
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
