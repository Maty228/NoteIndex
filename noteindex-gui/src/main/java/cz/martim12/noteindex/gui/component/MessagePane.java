package cz.martim12.noteindex.gui.component;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;


public final class MessagePane {

    private final VBox root;

    public MessagePane(String caption, String title, String message, Runnable closeAction) {
        Objects.requireNonNull(closeAction, "Close action must not be null");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("modal-caption");

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("modal-title");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("modal-message");

        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.getStyleClass().add("primary-button");
        okButton.setOnAction(event -> closeAction.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(spacer, okButton);
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
