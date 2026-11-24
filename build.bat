@echo off
REM Compiles the project without Maven (Windows). Output goes to the "out" folder.
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac --release 11 -encoding UTF-8 -d out -cp "lib/*" src\main\java\main\*.java src\main\java\model\*.java src\main\java\service\*.java src\main\java\repository\*.java src\main\java\database\*.java src\main\java\filehandling\*.java src\main\java\collections\*.java src\main\java\multithreading\*.java src\main\java\exceptions\*.java src\main\java\interfaces\*.java src\main\java\util\*.java
if errorlevel 1 exit /b 1
copy src\main\resources\schema.sql out\ > nul
echo Build successful. Run the program with run.bat
