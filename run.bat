@echo off
REM Runs the program (Windows). Run build.bat first.
cd /d "%~dp0"
java -cp "out;lib/*" main.Main
