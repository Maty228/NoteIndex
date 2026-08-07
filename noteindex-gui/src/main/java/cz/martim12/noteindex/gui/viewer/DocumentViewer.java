package cz.martim12.noteindex.gui.viewer;


import cz.martim12.noteindex.core.model.Document;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DocumentViewer {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    private final BorderPane root = new BorderPane();

    private final Label title = new Label();
    private final Label metadata = new Label();
    private final Label source = new Label();

    private final ToggleButton previewButton = new ToggleButton("Preview");
    private final ToggleButton sourceButton = new ToggleButton("Source");

    private final TextArea content = new TextArea();

    private Document document;

    public DocumentViewer() {
        root.getStyleClass().add("document-viewer");

        title.getStyleClass().add("viewer-title");
        metadata.getStyleClass().add("viewer-metadata");
        source.getStyleClass().add("viewer-source");

        source.setWrapText(true);

        ToggleGroup modeGroup = new ToggleGroup();
        previewButton.setToggleGroup(modeGroup);
        sourceButton.setToggleGroup(modeGroup);

        previewButton.setSelected(true);

        previewButton.getStyleClass().add("viewer-mode-button");
        sourceButton.getStyleClass().add("viewer-mode-button");

        previewButton.setOnAction(event -> renderContent());
        sourceButton.setOnAction(event -> renderContent());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox modeButtons = new HBox(4, previewButton, sourceButton);
        modeButtons.setAlignment(Pos.CENTER_RIGHT);

        VBox information = new VBox(4, title, metadata, source);

        HBox header = new HBox(16, information, spacer, modeButtons);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.getStyleClass().add("viewer-header");

        content.setEditable(false);
        content.setWrapText(true);
        content.getStyleClass().add("viewer-content");

        root.setTop(header);
        root.setCenter(content);

        showEmpty();
    }

    public Parent root() {
        return root;
    }

    public void showDocument(Document document) {
        this.document = document;

        title.setText(document.title());
        metadata.setText(formatLabel(document.format()) + " · Imported " + DATE_FORMATTER.format(document.importedAt()));

        source.setText(document.sourceUri());

        source.setTooltip(new Tooltip(document.sourceUri()));

        previewButton.setSelected(true);

        renderContent();
    }

    public void showEmpty() {
        document = null;

        title.setText("Document");
        metadata.setText("");
        source.setText("");
        content.clear();
    }

    private void renderContent(){
        if (document == null) {
            content.clear();
            return;
        }

        boolean sourceMode = sourceButton.isSelected();

        content.setText(sourceMode ? document.originalContent() : document.searchableContent());

        content.getStyleClass().removeAll("viewer-preview-content", "viewer-source-content");
        content.getStyleClass().add(sourceMode ? "viewer-source-content" : "viewer-preview-content");

        content.positionCaret(0);
    }

    private static String formatLabel(String format) {
        return switch (format) {
            case "text/plain" -> "TXT";
            case "text/markdown" -> "Markdown";
            default -> format;
        };
    }
}
