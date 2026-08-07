package cz.martim12.noteindex.gui.application;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiApplicationContextTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void opensServiceInPreparedDatabaseDirectory()
            throws Exception {

        Path databaseFile =
                temporaryDirectory
                        .resolve("nested")
                        .resolve("library")
                        .resolve("noteindex.db");

        StubNoteIndexService service =
                new StubNoteIndexService();

        AtomicReference<Path> openedDatabase =
                new AtomicReference<>();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        GuiApplicationContext context =
                new GuiApplicationContext(
                        path -> {
                            openedDatabase.set(path);
                            return service;
                        },
                        executor
                );

        try {
            NoteIndexService openedService =
                    context.start(databaseFile)
                            .get(
                                    3,
                                    TimeUnit.SECONDS
                            );

            assertSame(
                    service,
                    openedService
            );

            assertEquals(
                    databaseFile
                            .toAbsolutePath()
                            .normalize(),
                    openedDatabase.get()
            );

            assertTrue(
                    Files.isDirectory(
                            databaseFile.getParent()
                    )
            );

            assertEquals(
                    GuiLifecycleState.READY,
                    context.state()
            );

            assertSame(
                    service,
                    context.service()
                            .orElseThrow()
            );

        } finally {
            context.close();
        }

        assertTrue(service.closed);

        assertEquals(
                GuiLifecycleState.CLOSED,
                context.state()
        );
    }

    @Test
    void closesServiceIdempotently()
            throws Exception {

        StubNoteIndexService service =
                new StubNoteIndexService();

        GuiApplicationContext context =
                new GuiApplicationContext(
                        path -> service
                );

        context.start(
                        temporaryDirectory.resolve(
                                "noteindex.db"
                        )
                )
                .get(
                        3,
                        TimeUnit.SECONDS
                );

        context.close();
        context.close();

        assertEquals(
                1,
                service.closeCount
        );

        assertEquals(
                GuiLifecycleState.CLOSED,
                context.state()
        );
    }

    @Test
    void rejectsSecondStartup()
            throws Exception {

        StubNoteIndexService service =
                new StubNoteIndexService();

        GuiApplicationContext context =
                new GuiApplicationContext(
                        path -> service
                );

        try {
            context.start(
                            temporaryDirectory.resolve(
                                    "first.db"
                            )
                    )
                    .get(
                            3,
                            TimeUnit.SECONDS
                    );

            assertThrows(
                    IllegalStateException.class,
                    () -> context.start(
                            temporaryDirectory.resolve(
                                    "second.db"
                            )
                    )
            );

        } finally {
            context.close();
        }
    }

    @Test
    void recordsStartupFailure() {
        GuiApplicationContext context =
                new GuiApplicationContext(
                        path -> {
                            throw new IllegalStateException(
                                    "Database unavailable"
                            );
                        }
                );

        try {
            ExecutionException exception =
                    assertThrows(
                            ExecutionException.class,
                            () -> context.start(
                                            temporaryDirectory
                                                    .resolve(
                                                            "noteindex.db"
                                                    )
                                    )
                                    .get(
                                            3,
                                            TimeUnit.SECONDS
                                    )
                    );

            assertInstanceOf(
                    IllegalStateException.class,
                    exception.getCause()
            );

            assertEquals(
                    "Database unavailable",
                    exception.getCause()
                            .getMessage()
            );

            assertEquals(
                    GuiLifecycleState.FAILED,
                    context.state()
            );

            assertTrue(
                    context.service().isEmpty()
            );

        } finally {
            context.close();
        }
    }

    private static final class StubNoteIndexService
            implements NoteIndexService {

        private boolean closed;
        private int closeCount;

        @Override
        public Document importFile(Path source) {
            throw new AssertionError(
                    "Import must not be called"
            );
        }

        @Override
        public List<SearchResult> search(
                SearchQuery query,
                int limit
        ) {
            throw new AssertionError(
                    "Search must not be called"
            );
        }

        @Override
        public List<DocumentSummary> listDocuments() {
            throw new AssertionError(
                    "List must not be called"
            );
        }

        @Override
        public Optional<Document> findDocument(
                long documentId
        ) {
            throw new AssertionError(
                    "Find must not be called"
            );
        }

        @Override
        public boolean deleteDocument(
                long documentId
        ) {
            throw new AssertionError(
                    "Delete must not be called"
            );
        }

        @Override
        public Set<String> supportedImportExtensions() {
            throw new AssertionError(
                    "Formats must not be called"
            );
        }

        @Override
        public void close() {
            closed = true;
            closeCount++;
        }
    }
}