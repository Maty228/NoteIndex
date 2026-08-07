package cz.martim12.noteindex.gui.importflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportFileSupportTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void recognizesSupportedFilesCaseInsensitively() throws Exception {
        ImportFileSupport support = new ImportFileSupport(
                Set.of("txt", "md", "markdown")
        );

        Path markdown = Files.writeString(
                temporaryDirectory.resolve("lecture.MD"),
                "# Lecture"
        );

        Path text = Files.writeString(
                temporaryDirectory.resolve("notes.TXT"),
                "Notes"
        );

        assertTrue(support.isSupported(markdown));
        assertTrue(support.isSupported(text));
    }

    @Test
    void rejectsUnsupportedFilesAndDirectories() throws Exception {
        ImportFileSupport support = new ImportFileSupport(
                Set.of("txt", "md", "markdown")
        );

        Path image = Files.writeString(
                temporaryDirectory.resolve("diagram.png"),
                "fake image"
        );

        Path directory = Files.createDirectory(
                temporaryDirectory.resolve("folder")
        );

        assertFalse(support.isSupported(image));
        assertFalse(support.isSupported(directory));
    }

    @Test
    void detectsSupportedFileInsideMixedDrop() throws Exception {
        ImportFileSupport support = new ImportFileSupport(
                Set.of("txt", "md", "markdown")
        );

        Path markdown = Files.writeString(
                temporaryDirectory.resolve("lecture.md"),
                "# Lecture"
        );

        Path image = Files.writeString(
                temporaryDirectory.resolve("diagram.png"),
                "fake image"
        );

        assertTrue(
                support.containsSupportedFile(
                        List.of(image, markdown)
                )
        );
    }

    @Test
    void returnsOnlyRegularFilesFromDrop() throws Exception {
        ImportFileSupport support = new ImportFileSupport(
                Set.of("txt", "md")
        );

        Path text = Files.writeString(
                temporaryDirectory.resolve("notes.txt"),
                "Notes"
        );

        Path image = Files.writeString(
                temporaryDirectory.resolve("image.png"),
                "image"
        );

        Path directory = Files.createDirectory(
                temporaryDirectory.resolve("folder")
        );

        assertEquals(
                List.of(
                        text.toAbsolutePath().normalize(),
                        image.toAbsolutePath().normalize()
                ),
                support.regularFiles(
                        List.of(text, directory, image)
                )
        );
    }

    @Test
    void createsStableSupportedFormatsLabel() {
        ImportFileSupport support = new ImportFileSupport(
                Set.of("markdown", "txt", "md")
        );

        assertEquals(
                "TXT · MD · MARKDOWN",
                support.supportedFormatsLabel()
        );
    }
}