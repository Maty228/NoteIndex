#!/usr/bin/env bash

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATABASE="$ROOT/target/demo/noteindex.db"
SETUP_SCRIPT="$ROOT/scripts/setup-demo.sh"
GUI_POM="$ROOT/noteindex-gui/pom.xml"

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not available."
    echo "NoteIndex requires JDK 25."
    exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
    echo "Error: Maven is not available."
    exit 1
fi

if [[ ! -f "$DATABASE" ]]; then
    echo
    echo "The demo database does not exist yet."
    echo "Creating it now..."
    echo

    "$SETUP_SCRIPT"
fi

echo
echo "Starting NoteIndex"
echo "=================="
echo
echo "Database:"
echo "  $DATABASE"
echo

mvn \
    -f "$GUI_POM" \
    javafx:run \
    "-Djavafx.args=--database=$DATABASE"