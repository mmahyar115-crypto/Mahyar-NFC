@echo off
setlocal
set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_URL=https://services.gradle.org/distributions/gradle-9.5.0-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Gradle wrapper jar is missing; downloading official Gradle 9.5.0 wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 exit /b 1
)

if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
