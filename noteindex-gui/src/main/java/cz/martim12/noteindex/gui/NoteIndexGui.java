package cz.martim12.noteindex.gui;

import cz.martim12.noteindex.application.api.NoteIndexApplications;
import cz.martim12.noteindex.gui.application.GuiApplicationContext;
import cz.martim12.noteindex.gui.application.GuiDatabasePaths;
import cz.martim12.noteindex.gui.application.GuiLifecycleState;
import cz.martim12.noteindex.gui.main.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * JavaFX entry point for the NoteIndex desktop application.
 */
public final class NoteIndexGui extends Application {
    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 800;

    public static final double MINIMUM_WIDTH = 900;
    public static final double MINIMUM_HEIGHT = 600;

    private GuiApplicationContext applicationContext;
    private MainWindow mainWindow;

    @Override
    public void init() {
        applicationContext = new GuiApplicationContext(NoteIndexApplications::open);
    }

    @Override
    public void start(Stage primaryStage) {
        mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow.root(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        scene.getStylesheets().add(stylesheet("styles/base.css"));
        scene.getStylesheets().add(stylesheet("styles/light.css"));

        primaryStage.setTitle("NoteIndex");
        primaryStage.setMinWidth(MINIMUM_WIDTH);
        primaryStage.setMinHeight(MINIMUM_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();

        final Path databaseFile;

        try {
            databaseFile = resolveDatabaseFile();
        } catch (RuntimeException exception) {
            mainWindow.showStartupFailure(null, exception);

            showStartupFailureDialog(primaryStage, null, exception);

            return;
        }

        mainWindow.showStarting(databaseFile);

        try {
            applicationContext
                    .start(databaseFile)
                    .whenComplete(
                            (service, failure) ->
                                    Platform.runLater(
                                            () -> {
                                                if (applicationContext.state() == GuiLifecycleState.CLOSED) {
                                                    return;
                                                }

                                                if (failure == null) {
                                                    mainWindow.showReady(databaseFile);
                                                    return;

                                                }

                                                Throwable actualFailure = unwrapFailure(failure);

                                                mainWindow.showStartupFailure(databaseFile, actualFailure);
                                                showStartupFailureDialog(primaryStage, databaseFile, actualFailure);
                                            }
                                    ));
        } catch (RuntimeException exception) {
            mainWindow.showStartupFailure(databaseFile, exception);
            showStartupFailureDialog(primaryStage, databaseFile, exception);
        }
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    public static void main(String[] arguments) {
        launch(arguments);
    }

    private Path resolveDatabaseFile() {
        String configuredDatabase = getParameters().getNamed().get("database");

        if (configuredDatabase == null || configuredDatabase.isBlank()) {
            return GuiDatabasePaths.defaultDatabaseFile();
        }

        return Path.of(configuredDatabase).toAbsolutePath().normalize();
    }

    private void showStartupFailureDialog(Stage owner, Path databaseFile, Throwable failure) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Could not open NoteIndex");
        alert.setHeaderText("The NoteIndex library could not be opened.");

        StringBuilder message = new StringBuilder();

        if (databaseFile != null) {
            message.append("Database file:\n")
                    .append(databaseFile)
                    .append("\n\n");
        }

        message.append(displayMessage(failure));


        alert.setContentText(message.toString());

        alert.showAndWait();
    }

    private static Throwable unwrapFailure(Throwable failure) {
        Throwable current = Objects.requireNonNull(failure, "Failure must not be null");

        while ((current instanceof CompletionException || current instanceof ExecutionException)
        && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private static String displayMessage(Throwable failure) {
        Throwable current = failure;

        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }

            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }

        return failure.getClass().getSimpleName();
    }

    private static String stylesheet(String relativePath) {
        URL resource = Objects.requireNonNull(
                NoteIndexGui.class.getResource(relativePath),
                "Missing GUI stylesheet: " + relativePath);

        return resource.toExternalForm();
    }
}
