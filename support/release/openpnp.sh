#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi
if ! command -v "$JAVA" >/dev/null 2>&1; then
  echo 'Java 11 or newer is required. Install a full desktop Java runtime (not headless).' >&2
  exit 1
fi
exec "$JAVA" --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.color=ALL-UNNAMED -jar "$ROOT/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar" "$@"
