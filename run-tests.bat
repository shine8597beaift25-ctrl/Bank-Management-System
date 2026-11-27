@echo off
REM Runs the automated tests against a separate MySQL database (bank_management_test), re-created on every run.
cd /d "%~dp0"
call build.bat || exit /b 1
if not exist out-test mkdir out-test
javac --release 11 -encoding UTF-8 -d out-test -cp out src\test\java\test\BankSystemTestRunner.java || exit /b 1
java -Ddb.name=bank_management_test -cp "out;out-test;lib/*" test.BankSystemTestRunner
