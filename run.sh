#!/usr/bin/env bash
# ---------------------------------------------------------------
#  Builds and launches the Student Management System.
#
#  Usage:  ./run.sh                 use db.properties (SQLite by default)
#          ./run.sh --db my.db      a different SQLite file
#          ./run.sh --csv my.csv    the alternative CSV store
#          ./run.sh --test          run the test suite instead
#          ./run.sh --setup         download the JDBC jars into lib/
# ---------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  echo "[ERROR] javac was not found on your PATH. Install a JDK 8 or newer." >&2
  exit 1
fi

BASE="https://repo1.maven.org/maven2"
JARS=(
  "sqlite-jdbc-3.46.1.3.jar|org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar"
  "slf4j-api-2.0.16.jar|org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar"
  "slf4j-nop-2.0.16.jar|org/slf4j/slf4j-nop/2.0.16/slf4j-nop-2.0.16.jar"
)

download_jars() {
  mkdir -p lib
  for entry in "${JARS[@]}"; do
    name="${entry%%|*}"
    path="${entry##*|}"
    if [[ -f "lib/$name" ]]; then
      echo "  already present: $name"
    else
      curl -fsSL "$BASE/$path" -o "lib/$name"
      echo "  downloaded: $name"
    fi
  done
}

if [[ "${1:-}" == "--setup" ]]; then
  download_jars
  echo "[OK] lib/ is ready."
  exit 0
fi

if [[ ! -f "lib/sqlite-jdbc-3.46.1.3.jar" ]]; then
  echo "The JDBC driver is missing from lib/, fetching it..."
  download_jars
fi

# Each path is quoted: javac argfiles split on whitespace, and the project
# may live under a directory whose name contains spaces.
quote_paths() { sed -e 's/^/"/' -e 's/$/"/'; }

if [[ "${1:-}" == "--test" ]]; then
  mkdir -p out-test
  find src test -name '*.java' | quote_paths > out-test/sources.txt
  echo "Compiling tests..."
  javac -encoding UTF-8 -cp "lib/*" -d out-test @out-test/sources.txt
  exec java -cp "out-test:lib/*" com.sms.AppTests
fi

mkdir -p out
find src -name '*.java' | quote_paths > out/sources.txt
echo "Compiling..."
javac -encoding UTF-8 -cp "lib/*" -d out @out/sources.txt
echo "[OK] Starting Student Management System..."
exec java -cp "out:lib/*" com.sms.Main "$@"
