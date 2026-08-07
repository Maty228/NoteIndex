package cz.martim12.noteindex.gui.importflow;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import javafx.application.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Coordinates sequential document imports outside the JavaFX
 * Application Thread.
 */
public final class ImportCoordinator implements AutoCloseable {
    private final NoteIndexService service;
    private final ExecutorService executor;
    private final Consumer<Runnable> uiExecutor;

    private final AtomicBoolean importing = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public ImportCoordinator(NoteIndexService service) {
        this(service, createDefaultExecutor(), Platform::runLater);
    }

    ImportCoordinator(NoteIndexService service, ExecutorService executor, Consumer<Runnable> uiExecutor) {
        this.service = Objects.requireNonNull(service, "Service must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "UI executor must not be null");
    }

    public Set<String> supportedExtensions() {
        ensureOpen();
        return Set.copyOf(service.supportedImportExtensions());

    }

    public CompletableFuture<ImportBatchResult> importFiles(List<Path> sources, Consumer<ImportProgress> progressConsumer) {
        ensureOpen();

        Objects.requireNonNull(sources, "Sources must not be null");
        Objects.requireNonNull(progressConsumer, "Progress consumer must not be null");

        List<Path> normalizedSources = normalizeSources(sources);

        if (normalizedSources.isEmpty()) {
            throw new IllegalArgumentException("Sources must not be empty");
        }

        if (!importing.compareAndSet(false, true)) {
            throw new IllegalStateException("Import is already in progress");
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Document> importedDocuments = new ArrayList<>();
            List<ImportBatchResult.Failure> failures  = new ArrayList<>();

            for (int index = 0; index < normalizedSources.size(); index++) {
                if (closed.get()){
                    break;
                }

                Path source = normalizedSources.get(index);

                ImportProgress progress = new ImportProgress(index + 1, normalizedSources.size(), source);

                uiExecutor.accept(() -> progressConsumer.accept(progress));

                try {
                    Document imported = Objects.requireNonNull(service.importFile(source), "Application service returned null document");
                    importedDocuments.add(imported);
                } catch (RuntimeException exception) {
                    failures.add(new ImportBatchResult.Failure(source, displayMessage(exception)));

                }
            }

            return new ImportBatchResult(importedDocuments, failures);

        }, executor).whenComplete((result, failure) -> importing.set(false));
    }

    public boolean isImporting() {
        return importing.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdownNow();
    }

    private static List<Path> normalizeSources(List<Path> sources) {
        LinkedHashSet<Path> normalized = new LinkedHashSet<>();

        for (Path source : sources) {
            Path required = Objects.requireNonNull(source, "Source must not be null");

            normalized.add(required.toAbsolutePath().normalize());
        }

        return List.copyOf(normalized);
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Import coordinator is closed");
        }
    }

    private static String displayMessage(Throwable failure) {
        Throwable current = failure;

        while(current != null) {
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

    private static ExecutorService createDefaultExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "noteindex-gui-import");
            thread.setDaemon(true);
            return thread;
        });
    }

}
