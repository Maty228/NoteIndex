package cz.martim12.noteindex.gui.component;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Objects;
public final class ModalHost {

    private final StackPane root = new StackPane();
    private final StackPane contentHost = new StackPane();

    public ModalHost() {
        root.getStyleClass().add("modal-layer");
        contentHost.getStyleClass().add("modal-content-host");

        contentHost.setMaxSize(
                Region.USE_PREF_SIZE,
                Region.USE_PREF_SIZE
        );

        root.getChildren().addAll(contentHost);

        hide();
    }

    public Parent root() {
        return root;
    }

    public void show(Node content) {
        contentHost.getChildren().setAll(Objects.requireNonNull(content, "Modal content must not be null"));

        root.setVisible(true);
        root.setManaged(true);
        root.toFront();
    }

    public void hide() {
        root.setVisible(false);
        root.setManaged(false);

        contentHost.getChildren().clear();
    }

    public boolean isShowing() {
        return root.isVisible();
    }
}
