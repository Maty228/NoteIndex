package cz.martim12.noteindex.gui.search;

import cz.martim12.noteindex.core.model.SearchResult;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SearchResultCell extends ListCell<SearchResult>{

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    private final Label title = new Label();
    private final Label snippet = new Label();
    private final Label metadata = new Label();

    private final VBox content = new VBox(5, title, snippet, metadata);

    public SearchResultCell() {
        title.getStyleClass().add("search-result-title");

        snippet.setWrapText(true);
        snippet.getStyleClass().add("search-result-snippet");

        metadata.getStyleClass().add("search-result-metadata");

        content.getStyleClass().add("search-result-content");
    }

    @Override
    protected void updateItem(SearchResult result, boolean empty) {
        super.updateItem(result, empty);

        if (empty || result == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        title.setText(result.document().title());

        snippet.setText(normalizeSnippet(result.snippet()));

        metadata.setText(
                formatLabel(result.document().format() + " · " + DATE_FORMATTER.format(result.document().importedAt()) + " · Relevance " + String.format(Locale.ROOT, "%.2f", result.score())  )
        );

        setText(null);
        setGraphic(content);
    }

    private static String normalizeSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return "No preview available";
        }

        return snippet.replaceAll("\\s+", " ").trim();
    }

    private static String formatLabel(String format) {
        return switch (format) {
            case "text/plain" -> "TXT";
            case "text/markdown" -> "Markdown";
            default -> format;
        };
    }
}
