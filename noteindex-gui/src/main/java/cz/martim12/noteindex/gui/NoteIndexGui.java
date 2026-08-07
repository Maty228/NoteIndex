package cz.martim12.noteindex.gui;

import cz.martim12.noteindex.gui.main.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

/**
 * JavaFX entry point for the NoteIndex desktop application.
 */
public final class NoteIndexGui extends Application {
    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 800;

    public static final double MINIMUM_WIDTH = 900;
    public static final double MINIMUM_HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow.root(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        scene.getStylesheets().add(stylesheet("styles/base.css"));
        scene.getStylesheets().add(stylesheet("styles/light.css"));

        primaryStage.setTitle("NoteIndex");
        primaryStage.setMinWidth(MINIMUM_WIDTH);
        primaryStage.setMinHeight(MINIMUM_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] arguments) {
        launch(arguments);
    }

    private static String stylesheet(String relativePath) {
        URL resource = Objects.requireNonNull(
                NoteIndexGui.class.getResource(relativePath),
                "Missing GUI stylesheet: " + relativePath);

        return resource.toExternalForm();
    }
}
