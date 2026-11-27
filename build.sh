#!/bin/sh
# Compiles the project without Maven (Linux / macOS). Output goes to the "out" folder.
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac --release 11 -encoding UTF-8 -d out $(find src/main/java -name "*.java") || exit 1
cp src/main/resources/schema.sql out/
if ! ls lib/*.jar >/dev/null 2>&1; then
    echo "WARNING: no MySQL driver jar found in lib/. See lib/README.txt before running."
fi
echo "Build successful. Run the program with ./run.sh"
