/**
 * Provides persistent storage functionality for NoteIndex.
 *
 * <p>This module contains repository abstractions and JDBC-based SQLite
 * persistence implementations.</p>
 */
module noteindex.persistence {
    requires transitive noteindex.core;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports cz.martim12.noteindex.persistence.api;
    exports cz.martim12.noteindex.persistence.exception;
    exports cz.martim12.noteindex.persistence.jdbc;
}