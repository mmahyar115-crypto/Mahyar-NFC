#!/usr/bin/env sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://services.gradle.org/distributions/gradle-9.5.0-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper jar is missing; downloading official Gradle 9.5.0 wrapper..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$WRAPPER_URL" -o "$WRAPPER_JAR" || exit 1
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$WRAPPER_JAR" "$WRAPPER_URL" || exit 1
  else
    echo "Please install curl/wget or open the project in Android Studio."
    exit 1
  fi
fi

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
