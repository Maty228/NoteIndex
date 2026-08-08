package cz.martim12.noteindex.gui.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

public final class TextInputPane {

    private final VBox root;

    public TextInputPane(String caption, String title, String initialValue, String confirmText, Runnable cancelAction, Consumer<String> confirmAction) {

        Objects.requireNonNull(initialValue, "Initial value must not be null");
        Objects.requireNonNull(cancelAction, "Cancel action must not be null");
        Objects.requireNonNull(confirmAction, "Confirm action must not be null");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("modal-caption");

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("modal-title");

        TextField input = new TextField(initialValue);
        input.getStyleClass().add("modal-text-field");

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> cancelAction.run());

        Button confirmButton = new Button(confirmText);
        confirmButton.setDefaultButton(true);
        confirmButton.getStyleClass().add("primary-button");

        confirmButton.setOnAction(event -> {
            String value = input.getText().trim();

            if (!value.isBlank()) {
                confirmAction.accept(value);
            }
        });

        input.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        confirmButton.setDisable(newValue == null || newValue.isBlank() || newValue.trim().equals(initialValue.trim()))
        );

        confirmButton.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(
                10, spacer, cancelButton, confirmButton
        );

        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("modal-actions");

        root = new VBox(
                12, captionLabel, titleLabel, input, actions
        );

        root.setPrefWidth(460);
        root.setMaxSize(460, Region.USE_PREF_SIZE);

        root.getStyleClass().add("modal-card");

        Platform.runLater(() -> {
            input.requestFocus();
            input.selectAll();
        });
    }

    public Parent root() {
        return root;
    }
}