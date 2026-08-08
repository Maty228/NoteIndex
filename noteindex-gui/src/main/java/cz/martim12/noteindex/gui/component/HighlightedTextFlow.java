package cz.martim12.noteindex.gui.component;

import cz.martim12.noteindex.core.model.HighlightRange;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class HighlightedTextFlow extends StackPane {

    private final Pane highlightLayer = new Pane();
    private final TextFlow textFlow = new TextFlow();

    private List<HighlightRange> highlights = List.of();

    private boolean updatingHighlights;

    public HighlightedTextFlow() {
        getStyleClass().add("highlighted-text-flow");

        highlightLayer.getStyleClass().add(
                "highlight-background-layer"
        );

        highlightLayer.setMouseTransparent(true);

        textFlow.getStyleClass().add(
                "highlight-text-layer"
        );

        setAlignment(javafx.geometry.Pos.TOP_LEFT);

        getChildren().addAll(
                highlightLayer,
                textFlow
        );
    }

    public void showText(
            String text,
            List<HighlightRange> highlights
    ) {
        Objects.requireNonNull(
                text,
                "Text must not be null"
        );

        Objects.requireNonNull(
                highlights,
                "Highlights must not be null"
        );

        this.highlights =
                mergeRanges(text, highlights);

        textFlow.getChildren().clear();

        if (text.isEmpty()) {
            highlightLayer.getChildren().clear();
            return;
        }

        if (this.highlights.isEmpty()) {
            textFlow.getChildren().add(
                    createText(text, false)
            );

            requestLayout();
            return;
        }

        int position = 0;

        for (HighlightRange range :
                this.highlights) {

            if (range.startOffset() > position) {
                textFlow.getChildren().add(
                        createText(
                                text.substring(
                                        position,
                                        range.startOffset()
                                ),
                                false
                        )
                );
            }

            textFlow.getChildren().add(
                    createText(
                            text.substring(
                                    range.startOffset(),
                                    range.endOffset()
                            ),
                            true
                    )
            );

            position = range.endOffset();
        }

        if (position < text.length()) {
            textFlow.getChildren().add(
                    createText(
                            text.substring(position),
                            false
                    )
            );
        }

        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        rebuildHighlightBackgrounds();
    }

    private void rebuildHighlightBackgrounds() {
        if (updatingHighlights) {
            return;
        }

        updatingHighlights = true;

        try {
            highlightLayer.getChildren().clear();

            if (highlights.isEmpty()) {
                return;
            }

            for (HighlightRange range : highlights) {
                PathElement[] elements =
                        textFlow.getRangeShape(
                                range.startOffset(),
                                range.endOffset(),
                                false
                        );

                if (elements.length == 0) {
                    continue;
                }

                Path background =
                        new Path(elements);

                background.setManaged(false);

                background.getStyleClass().add(
                        "search-highlight-background"
                );

                highlightLayer.getChildren().add(
                        background
                );
            }
        } finally {
            updatingHighlights = false;
        }
    }

    private static Text createText(
            String value,
            boolean highlighted
    ) {
        Text text = new Text(value);

        text.getStyleClass().add(
                "highlighted-text"
        );

        if (highlighted) {
            text.getStyleClass().add(
                    "highlighted-text-match"
            );
        }

        return text;
    }

    private static List<HighlightRange> mergeRanges(
            String text,
            List<HighlightRange> highlights
    ) {
        List<HighlightRange> valid =
                highlights.stream()
                        .filter(range ->
                                range.startOffset()
                                        < text.length()
                                        && range.endOffset()
                                        <= text.length()
                        )
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                HighlightRange::startOffset
                                        )
                                        .thenComparingInt(
                                                HighlightRange::endOffset
                                        )
                        )
                        .toList();

        if (valid.isEmpty()) {
            return List.of();
        }

        List<HighlightRange> merged =
                new ArrayList<>();

        int start =
                valid.getFirst().startOffset();

        int end =
                valid.getFirst().endOffset();

        for (
                int index = 1;
                index < valid.size();
                index++
        ) {
            HighlightRange next =
                    valid.get(index);

            if (next.startOffset() <= end) {
                end = Math.max(
                        end,
                        next.endOffset()
                );

                continue;
            }

            merged.add(
                    new HighlightRange(start, end)
            );

            start = next.startOffset();
            end = next.endOffset();
        }

        merged.add(
                new HighlightRange(start, end)
        );

        return List.copyOf(merged);
    }
}