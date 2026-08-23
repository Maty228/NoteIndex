/**
 * Provides application-level workflows and services.
 *
 * <p>This module coordinates importing, document management, searching,
 * and synchronization between application components.</p>
 */
module noteindex.application {
    requires transitive noteindex.core;

    requires noteindex.importer;
    requires noteindex.persistence;
    requires noteindex.search;

    exports cz.martim12.noteindex.application.api;
}