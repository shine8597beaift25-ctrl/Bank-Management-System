#!/bin/sh
# Compiles and runs the automated test runner against a separate test database.
cd "$(dirname "$0")"
./build.sh || exit 1
mkdir -p out-test
javac --release 11 -encoding UTF-8 -d out-test -cp "out:lib/*" src/test/java/test/BankSystemTestRunner.java || exit 1
java -Dbank.db.path=data/test_bank.db -cp "out:out-test:lib/*" test.BankSystemTestRunner
