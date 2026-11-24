#!/bin/sh
# Compiles the project without Maven (Linux / macOS). Output goes to the "out" folder.
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac --release 11 -encoding UTF-8 -d out -cp "lib/*" $(find src/main/java -name "*.java") || exit 1
cp src/main/resources/schema.sql out/
echo "Build successful. Run the program with ./run.sh"
