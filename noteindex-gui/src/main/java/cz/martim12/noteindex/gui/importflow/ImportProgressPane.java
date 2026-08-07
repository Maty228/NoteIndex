package cz.martim12.noteindex.gui.importflow;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

public final class ImportProgressPane {
    private final VBox root;

    private final Label title = new Label("Importing notes");
    private final Label summary = new Label();
    private final Label currentFile = new Label();

    private final ProgressBar progressBar = new ProgressBar(0);

    private final VBox failures = new VBox(10);
    private final ScrollPane failuresScroll = new ScrollPane(failures);

    private final Button okButton = new Button("OK");

    private Runnable closeAction = () -> {};

    public ImportProgressPane(int totalFiles) {
        if (totalFiles <= 0) {
            throw new IllegalArgumentException("Total file count must be positive");
        }

        Label caption = new Label("Import notes");
        caption.getStyleClass().add("modal-caption");

        title.getStyleClass().add("modal-title");

        summary.setText("Preparing " + totalFiles + (totalFiles == 1 ? " file…" : " files…"));

        summary.getStyleClass().add("modal-message");

        currentFile.getStyleClass().add("import-current-file");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("noteindex-progress-bar");

        failures.getStyleClass().add("import-failures");

        failuresScroll.setFitToWidth(true);
        failuresScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        failuresScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        failuresScroll.setVisible(false);
        failuresScroll.setManaged(false);
        failuresScroll.getStyleClass().add("import-failures-scroll");

        okButton.setDefaultButton(true);
        okButton.setVisible(false);
        okButton.setManaged(false);
        okButton.getStyleClass().add("primary-button");
        okButton.setOnAction(event -> closeAction.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(spacer, okButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("modal-actions");

        root = new VBox(
                12, caption, title, summary, currentFile, progressBar, failuresScroll, actions
        );

        root.setPrefWidth(560);
        root.setMaxSize(560, Region.USE_PREF_SIZE);
        root.getStyleClass().add("modal-card");
    }

    public Parent root() {
        return root;
    }

    public void setOnClose(Runnable closeAction) {
        this.closeAction = Objects.requireNonNull(
                closeAction,
                "Close action must not be null"
        );
    }

    public void update(ImportProgress progress) {
        summary.setText("Importing " + progress.current() + " of " + progress.total());

        currentFile.setText(progress.source().getFileName().toString());
        progressBar.setProgress(progress.fraction());
    }

    public void showResult(ImportBatchResult result) {
        int successful = result.importedDocuments().size();
        int failed = result.failures().size();

        progressBar.setProgress(1.0);

        if (failed == 0) {
            title.setText("Import complete");

            summary.setText(
                    successful == 1
                            ? "1 note was imported successfully."
                            : successful + " notes were imported successfully."
            );

        } else if (successful == 0) {
            title.setText("Import failed");

            summary.setText(
                    result.totalProcessed() == 1
                            ? "The selected note could not be imported."
                            : "None of the selected notes could be imported."
            );

        } else {
            title.setText("Import completed with some problems");

            summary.setText(
                    successful + " of " + result.totalProcessed()
                            + " notes were imported successfully."
            );
        }

        currentFile.setVisible(false);
        currentFile.setManaged(false);

        if (result.hasFailures()) {
            populateFailures(result);

            if (result.failures().size() > 3) {
                failuresScroll.setPrefHeight(220);
                failuresScroll.setMaxHeight(220);
            } else {
                failuresScroll.setPrefHeight(Region.USE_COMPUTED_SIZE);
                failuresScroll.setMaxHeight(Region.USE_PREF_SIZE);
            }

            failuresScroll.setVisible(true);
            failuresScroll.setManaged(true);
        }

        okButton.setVisible(true);
        okButton.setManaged(true);
        okButton.requestFocus();
    }

    private void populateFailures(ImportBatchResult result) {
        failures.getChildren().clear();

        Label heading = new Label("Files that could not be imported");
        heading.getStyleClass().add("import-failures-title");

        failures.getChildren().add(heading);

        for (ImportBatchResult.Failure failure : result.failures()) {
            Label file = new Label(failure.source().getFileName().toString());
            file.getStyleClass().add("import-failure-file");

            Label reason = new Label(failure.message());
            reason.setWrapText(true);
            reason.getStyleClass().add("import-failure-reason");

            VBox failureBox = new VBox(3, file, reason);
            failureBox.getStyleClass().add("import-failure");

            failures.getChildren().add(failureBox);
        }
    }
}
