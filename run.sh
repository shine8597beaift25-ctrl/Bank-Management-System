#!/bin/sh
# Runs the program (Linux / macOS). Run ./build.sh first.
# Needs: a running MySQL server, settings in db.properties, and the MySQL driver jar in lib/.
cd "$(dirname "$0")"
java -cp "out:lib/*" main.Main
