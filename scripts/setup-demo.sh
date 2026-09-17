#!/usr/bin/env bash

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NOTES_DIR="$ROOT/examples/demo-notes"
DEMO_DIR="$ROOT/target/demo"
DATABASE="$DEMO_DIR/noteindex.db"

CLI_POM="$ROOT/noteindex-cli/pom.xml"
CLI_MAIN="cz.martim12.noteindex.cli.NoteIndexCli"

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not available."
    echo "NoteIndex requires JDK 25."
    exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
    echo "Error: Maven is not available."
    exit 1
fi

if [[ ! -d "$NOTES_DIR" ]]; then
    echo "Error: Demo notes directory was not found:"
    echo "  $NOTES_DIR"
    exit 1
fi

shopt -s nullglob
DEMO_FILES=(
    "$NOTES_DIR"/*.txt
    "$NOTES_DIR"/*.md
    "$NOTES_DIR"/*.markdown
)
shopt -u nullglob

if [[ ${#DEMO_FILES[@]} -eq 0 ]]; then
    echo "Error: No supported demo documents were found in:"
    echo "  $NOTES_DIR"
    exit 1
fi

run_cli() {
    local arguments="$1"

    mvn -q \
        -f "$CLI_POM" \
        exec:java \
        "-Dexec.mainClass=$CLI_MAIN" \
        "-Dexec.args=$arguments"
}

echo
echo "NoteIndex demo setup"
echo "===================="
echo
echo "Demo documents: ${#DEMO_FILES[@]}"
echo "Demo database:"
echo "  $DATABASE"
echo

echo "Installing NoteIndex modules..."
mvn -q -f "$ROOT/pom.xml" -DskipTests install

echo
echo "Creating a fresh demo database..."

# Only the generated demo directory is removed.
# The normal ~/.noteindex database is never touched.
rm -rf "$DEMO_DIR"
mkdir -p "$DEMO_DIR"

echo
echo "Importing demo documents..."

for file in "${DEMO_FILES[@]}"; do
    echo "  $(basename "$file")"

    run_cli "--database \"$DATABASE\" import \"$file\""
done

echo
echo "Demo database created successfully."
echo

echo "Imported documents"
echo "------------------"
run_cli "--database \"$DATABASE\" list"

echo
echo "Example search: virtual"
echo "-----------------------"
run_cli "--database \"$DATABASE\" search virtual"

echo
echo "Setup complete."
echo
echo "The generated demo database is:"
echo "  $DATABASE"
echo
echo "Open it in the JavaFX application with:"
echo "  ./scripts/run-demo-gui.sh"
echo