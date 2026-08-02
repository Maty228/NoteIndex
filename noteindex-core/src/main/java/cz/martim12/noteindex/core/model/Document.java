package cz.martim12.noteindex.core.model;

import java.time.Instant;
public record Document (
    long id,
    String title,
    String sourceUri,
    String format,
    String originalContent,
    String searchableContent,
    Instant importedAt

    ){}
