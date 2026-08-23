package cz.martim12.noteindex.gui.library;

import cz.martim12.noteindex.core.model.DocumentSummary;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX list cell displaying a document summary.
 */
public final class DocumentListCell extends ListCell<DocumentSummary>{
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    private final Label title = new Label();
    private final Label metadata = new Label();
    private final VBox content = new VBox(5, title, metadata);


    /**
     * Creates a document list cell.
     */
    public DocumentListCell() {
        title.getStyleClass().add("document-cell-title");
        metadata.getStyleClass().add("document-cell-metadata");

        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("document-cell-content");
    }

    @Override
    protected void updateItem(DocumentSummary document, boolean empty) {
        super.updateItem(document, empty);

        if (empty || document == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        title.setText(document.title());

        metadata.setText(
                formatLabel(document.format()) + " · " + DATE_FORMATTER.format(document.importedAt())
        );

        setText(null);
        setGraphic(content);
    }

    private static String formatLabel(String format) {
        return switch(format) {
            case "text/plain" -> "TXT";
            case "text/markdown" -> "Markdown";
            default -> format;
        };
    }
}
