@echo off
REM Compiles and runs the automated test runner against a separate test database (Windows).
cd /d "%~dp0"
call build.bat || exit /b 1
if not exist out-test mkdir out-test
javac --release 11 -encoding UTF-8 -d out-test -cp "out;lib/*" src\test\java\test\BankSystemTestRunner.java || exit /b 1
java -Dbank.db.path=data/test_bank.db -cp "out;out-test;lib/*" test.BankSystemTestRunner
