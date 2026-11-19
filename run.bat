@echo off
rem Compile lalu jalankan. Butuh JDK 11 atau lebih baru (javac harus ada).
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 pause & exit /b 1
java -cp out GameApp
pause
