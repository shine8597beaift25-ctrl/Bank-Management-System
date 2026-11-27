@echo off
REM Runs the program (Windows). Run build.bat first.
REM Needs: a running MySQL server, settings in db.properties, and the MySQL driver jar in lib\.
cd /d "%~dp0"
java -cp "out;lib/*" main.Main
