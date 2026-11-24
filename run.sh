#!/bin/sh
# Runs the program (Linux / macOS). Run ./build.sh first.
cd "$(dirname "$0")"
java -cp "out:lib/*" main.Main
