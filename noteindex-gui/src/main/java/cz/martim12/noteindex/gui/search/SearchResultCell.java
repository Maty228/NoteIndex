package cz.martim12.noteindex.gui.search;

import cz.martim12.noteindex.core.model.SearchResult;
import cz.martim12.noteindex.gui.component.HighlightedTextFlow;
import cz.martim12.noteindex.core.model.HighlightRange;
import javafx.scene.shape.Rectangle;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SearchResultCell extends ListCell<SearchResult>{

    private static final int MAX_SNIPPET_CHARACTERS = 105;
    private static final int CONTEXT_BEFORE_MATCH = 45;
    private static final int WORD_BOUNDARY_LOOKBACK = 35;
    private static final double SNIPPET_HEIGHT = 48;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    private final Label title = new Label();
    private final HighlightedTextFlow snippet = new HighlightedTextFlow();
    private final Label metadata = new Label();

    private final VBox content = new VBox(5, title, snippet, metadata);

    public SearchResultCell() {
        title.getStyleClass().add("search-result-title");

        snippet.getStyleClass().add("search-result-snippet");

        snippet.setMaxWidth(Double.MAX_VALUE);
        snippet.setMinHeight(SNIPPET_HEIGHT);
        snippet.setPrefHeight(SNIPPET_HEIGHT);
        snippet.setMaxHeight(SNIPPET_HEIGHT);

        Rectangle snippetClip = new Rectangle();

        snippetClip.widthProperty().bind(
                snippet.widthProperty()
        );

        snippetClip.heightProperty().bind(
                snippet.heightProperty()
        );

        snippet.setClip(snippetClip);

        metadata.getStyleClass().add("search-result-metadata");

        content.getStyleClass().add("search-result-content");

        content.setMinWidth(0);
        snippet.setMinWidth(0);

        content.prefWidthProperty().bind(
                widthProperty().subtract(24)
        );

        content.maxWidthProperty().bind(
                widthProperty().subtract(24)
        );

        snippet.prefWidthProperty().bind(
                content.widthProperty()
        );

        snippet.maxWidthProperty().bind(
                content.widthProperty()
        );
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

        DisplayedSnippet displayedSnippet =
                displayedSnippet(result);

        snippet.showText(
                displayedSnippet.text(),
                displayedSnippet.highlights()
        );

        metadata.setText(
                formatLabel(result.document().format())
                        + " · " + DATE_FORMATTER.format(result.document().importedAt())
                        + " · Relevance " + String.format(Locale.ROOT, "%.2f", result.score())
        );

        setText(null);
        setGraphic(content);
    }

    private static DisplayedSnippet displayedSnippet(SearchResult result) {
        String text = result.snippet();

        if (text == null || text.isBlank()) {
            return new DisplayedSnippet(
                    "No preview available",
                    List.of()
            );
        }

        if (text.length() <= MAX_SNIPPET_CHARACTERS) {
            return new DisplayedSnippet(
                    text,
                    result.snippetHighlights()
            );
        }

        HighlightRange firstHighlight =
                result.snippetHighlights().isEmpty()
                        ? null
                        : result.snippetHighlights().getFirst();

        int start;

        if (firstHighlight == null) {
            start = 0;
        } else {
            start = Math.max(
                    0,
                    firstHighlight.startOffset() - CONTEXT_BEFORE_MATCH
            );
        }

        int end = Math.min(
                text.length(),
                start + MAX_SNIPPET_CHARACTERS
        );

        if (end - start < MAX_SNIPPET_CHARACTERS) {
            start = Math.max(
                    0,
                    end - MAX_SNIPPET_CHARACTERS
            );
        }


        start = alignSnippetStart(text, start);
        end = alignSnippetEnd(text, end);

        boolean truncatedStart = start > 0;
        boolean truncatedEnd = end < text.length();

        String prefix = truncatedStart ? "…" : "";
        String suffix = truncatedEnd ? "…" : "";

        String visibleText =
                prefix
                        + text.substring(start, end)
                        + suffix;

        int offset = prefix.length() - start;

        int finalStart = start;
        int finalEnd = end;
        List<HighlightRange> visibleHighlights =
                result.snippetHighlights()
                        .stream()
                        .filter(range ->
                                range.startOffset() >= finalStart
                                        && range.endOffset() <= finalEnd
                        )
                        .map(range ->
                                new HighlightRange(
                                        range.startOffset() + offset,
                                        range.endOffset() + offset
                                )
                        )
                        .toList();

        return new DisplayedSnippet(
                visibleText,
                visibleHighlights
        );
    }

    private static int alignSnippetStart(
            String text,
            int start
    ) {
        while (start > 0
                && start < text.length()
                && !Character.isWhitespace(
                text.charAt(start - 1)
        )) {

            start--;
        }

        return start;
    }

    private static int alignSnippetEnd(
            String text,
            int end
    ) {
        while (end < text.length()
                && !Character.isWhitespace(
                text.charAt(end)
        )) {

            end++;
        }

        return end;
    }

    private record DisplayedSnippet(
            String text,
            List<HighlightRange> highlights
    ) {}

    private static int findSnippetEnd(String text) {
        int end = Math.min(
                MAX_SNIPPET_CHARACTERS,
                text.length()
        );

        int minimumEnd = Math.max(
                0,
                end - WORD_BOUNDARY_LOOKBACK
        );

        while (end > minimumEnd
                && !Character.isWhitespace(
                text.charAt(end - 1)
        )) {

            end--;
        }

        if (end == minimumEnd) {
            return Math.min(
                    MAX_SNIPPET_CHARACTERS,
                    text.length()
            );
        }

        return end;
    }




    private static String formatLabel(String format) {
        return switch (format) {
            case "text/plain" -> "TXT";
            case "text/markdown" -> "Markdown";
            default -> format;
        };
    }
}
