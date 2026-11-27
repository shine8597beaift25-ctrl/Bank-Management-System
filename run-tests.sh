#!/bin/sh
# Compiles and runs the automated test runner against a separate MySQL database
# (bank_management_test), which is deleted and re-created on every run.
cd "$(dirname "$0")"
./build.sh || exit 1
mkdir -p out-test
javac --release 11 -encoding UTF-8 -d out-test -cp out src/test/java/test/BankSystemTestRunner.java || exit 1
java -Ddb.name=bank_management_test -cp "out:out-test:lib/*" test.BankSystemTestRunner
