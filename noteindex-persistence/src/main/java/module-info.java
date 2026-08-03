module noteindex.persistence {
    requires noteindex.core;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports cz.martim12.noteindex.persistence.api;
    exports cz.martim12.noteindex.persistence.exception;
    exports cz.martim12.noteindex.persistence.jdbc;
}