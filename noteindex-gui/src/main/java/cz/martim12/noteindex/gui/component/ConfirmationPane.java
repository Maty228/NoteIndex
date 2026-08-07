package cz.martim12.noteindex.gui.component;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;
public final class ConfirmationPane {

    private final VBox root;

    public ConfirmationPane(String caption, String title, String message, String confirmText, Runnable cancelAction, Runnable confirmAction) {
        Objects.requireNonNull(cancelAction, "Cancel action must not be null");
        Objects.requireNonNull(confirmAction, "Confirm action must not be null");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("modal-caption");

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("modal-title");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("modal-message");

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> cancelAction.run());

        Button confirmButton = new Button(confirmText);
        confirmButton.getStyleClass().add("danger-button");
        confirmButton.setOnAction(event -> confirmAction.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(10, spacer, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("modal-actions");

        root = new VBox(
                12, captionLabel, titleLabel, messageLabel, actions
        );

        root.setPrefWidth(460);
        root.setMaxSize(460, Region.USE_PREF_SIZE);
        root.getStyleClass().add("modal-card");
    }

    public Parent root() {
        return root;
    }
}
