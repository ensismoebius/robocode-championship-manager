#!/bin/sh
# Builds the program when needed and runs it
#
# Usage: ./run.sh [--build] [--test]
#   --build  rebuilds even if nothing changed
#   --test   also runs the automated tests when building

set -e
cd "$(dirname "$0")"

JAR=target/RobocodeChampManager-5-all.jar
FORCE=0
SKIP_TESTS=-DskipTests

for arg in "$@"; do
	case "$arg" in
	--build) FORCE=1 ;;
	--test) SKIP_TESTS= ;;
	-h | --help)
		sed -n '2,6s/^# \{0,1\}//p' "$0"
		exit 0
		;;
	*)
		echo "Unknown option: $arg (see --help)" >&2
		exit 1
		;;
	esac
done

if ! command -v java >/dev/null 2>&1; then
	echo "Java not found, install Java 21 or newer" >&2
	exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
if [ -n "$JAVA_VERSION" ] && [ "$JAVA_VERSION" -lt 21 ]; then
	echo "Java $JAVA_VERSION found, Java 21 or newer is needed" >&2
	exit 1
fi

# Rebuilds if the jar is missing or older than any source
if [ "$FORCE" = 1 ] || [ ! -f "$JAR" ] || [ -n "$(find pom.xml src -newer "$JAR" -print -quit)" ] || { [ -z "$SKIP_TESTS" ] && [ -n "$(find test -newer "$JAR" -print -quit)" ]; }; then
	if ! command -v mvn >/dev/null 2>&1; then
		echo "Maven not found, it is needed to build the program" >&2
		exit 1
	fi
	echo "Building..."
	mvn -q -B package $SKIP_TESTS
fi

# A leftover instance holds the port of the registration web server
if pgrep -f "java .*-jar $JAR" >/dev/null 2>&1; then
	echo "The program is already running (pid $(pgrep -f "java .*-jar $JAR" | tr '\n' ' '))." >&2
	echo "Close it or end it with: pkill -f RobocodeChampManager" >&2
	exit 1
fi

exec java -jar "$JAR"
