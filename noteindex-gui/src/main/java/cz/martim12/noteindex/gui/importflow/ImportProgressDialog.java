package cz.martim12.noteindex.gui.importflow;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class ImportProgressDialog {

    private final Dialog<Void> dialog = new Dialog<>();
    private final Label progressLabel = new Label();
    private final Label fileLabel = new Label();
    private final Label resultLabel = new Label();

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Button okButton;

    private boolean finished;

    public ImportProgressDialog(Window owner, int totalFiles) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Import notes");
        dialog.setHeaderText("Importing notes");

        progressLabel.setText("Preparing " + totalFiles + (totalFiles == 1? " file..." : "files..."));

        fileLabel.setWrapText(true);
        fileLabel.setMaxWidth(460);

        resultLabel.setWrapText(true);
        resultLabel.setMaxWidth(460);
        resultLabel.setVisible(false);
        resultLabel.setManaged(false);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefWidth(460);

        VBox content = new VBox(12, progressLabel, fileLabel, progressBar, resultLabel);

        content.setPadding(new Insets(8, 0, 4, 0));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        dialog.setOnCloseRequest(event -> {
            if (!finished) {
                event.consume();
            }
        });


    }

    public void show() {
        dialog.show();
    }

    public void update(ImportProgress progress) {
        if (finished) {
            return;
        }
        progressLabel.setText("Importing " + progress.current() + " of " + progress.total());

        fileLabel.setText(progress.source().getFileName().toString());
        progressBar.setProgress(progress.fraction());

    }

    public void showResult(ImportBatchResult result) {
        finished = true;

        int successful = result.importedDocuments().size();
        int failed = result.failures().size();

        dialog.setHeaderText(createHeader(successful, failed));

        progressLabel.setText(createSummary(successful, failed, result.totalProcessed()));

        fileLabel.setVisible(false);
        fileLabel.setManaged(false);

        progressBar.setProgress(1.0);

        if (result.hasFailures()) {
            resultLabel.setText(createFailureDetails(result));
            resultLabel.setVisible(true);
            resultLabel.setManaged(true);
        } else {
            resultLabel.setVisible(false);
            resultLabel.setManaged(false);
        }

        okButton.setDisable(false);
        okButton.requestFocus();



    }

    private static String createHeader(int successful, int failed) {
        if (failed == 0) {
            return "Import complete";
        }

        if (successful == 0) {
            return "Import failed";
        }

        return "Import completed with some problems";
    }

    private static String createSummary(int successful, int failed, int total) {
        if (failed == 0) {
            return successful == 1
                    ? "1 note was imported successfully."
                    : successful + " notes were imported successfully.";
        }

        if (successful == 0) {
            return total == 1
                    ? "The note could not be imported."
                    : "None of the " + total + " selected notes could be imported.";
        }

        return successful + " of " + total + " notes were imported successfully. "
                + failed + (failed == 1 ? " file failed." : " files failed.");
    }

    private static String createFailureDetails(ImportBatchResult result) {
        StringBuilder details = new StringBuilder("Failed files:");

        for (ImportBatchResult.Failure failure : result.failures()) {
            details.append("\n\n")
                    .append(failure.source().getFileName())
                    .append("\n")
                    .append(failure.message());
        }

        return details.toString();
    }

    public void close() {
        dialog.hide();
    }
}
