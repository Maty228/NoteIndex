package cz.martim12.noteindex.persistence.api;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    Document save(ImportedDocument document);

    Optional<Document> findById(long id);

    List<Document> findAll();
    List<DocumentSummary> findAllSummaries();

    boolean existsBySourceUri(String sourceUri);

    boolean deleteById(long id);

    boolean updateDisplayTitle(long id, String displayTitle);
}
