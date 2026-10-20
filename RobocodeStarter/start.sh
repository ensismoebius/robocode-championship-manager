#!/bin/sh
# Usage: start.sh <robocode dir> <rounds> <robot> <robot> [robot...]
cd "$(dirname "$0")" || exit 1
robocode="$1"
rounds="$2"
shift 2
# The java of JAVA_HOME (set by the program), or the one in the PATH
java=java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	java="$JAVA_HOME/bin/java"
fi
# libs/* : since 1.9.4 robocode.jar only has the API, the engine is in the other jars
exec "$java" -cp "$robocode/libs/*:." -Xmx512M -XX:+IgnoreUnrecognizedVMOptions \
	"--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" \
	"--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" \
	"--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED" \
	"--add-opens=java.desktop/sun.awt=ALL-UNNAMED" \
	BattleStarter -p "$robocode" -r "$rounds" -o "$@"
