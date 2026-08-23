package cz.martim12.noteindex.gui.component;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Objects;

/**
 * Container responsible for displaying modal content above the main view.
 */
public final class ModalHost {

    private final StackPane root = new StackPane();
    private final StackPane contentHost = new StackPane();

    /**
     * Creates an empty modal host.
     */
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

    /**
     * Returns the root node of this modal host.
     *
     * @return modal host root
     */
    public Parent root() {
        return root;
    }

    /**
     * Displays modal content.
     *
     * @param content node to display as modal content
     */
    public void show(Node content) {
        contentHost.getChildren().setAll(Objects.requireNonNull(content, "Modal content must not be null"));

        root.setVisible(true);
        root.setManaged(true);
        root.toFront();
    }

    /**
     * Hides the currently displayed modal content.
     */
    public void hide() {
        root.setVisible(false);
        root.setManaged(false);

        contentHost.getChildren().clear();
    }

    /**
     * Checks whether modal content is currently visible.
     *
     * @return true if a modal is shown
     */
    public boolean isShowing() {
        return root.isVisible();
    }
}
