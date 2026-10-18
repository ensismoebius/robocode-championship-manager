@echo off
rem Usage: start.bat <robocode dir> <rounds> <robot> <robot> [robot...]
rem libs\* : since 1.9.4 robocode.jar only has the API, the engine is in the other jars
cd /d "%~dp0"
java -cp ".;%~1\libs\*" -Xmx512M -XX:+IgnoreUnrecognizedVMOptions "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED" "--add-opens=java.desktop/sun.awt=ALL-UNNAMED" BattleStarter -p "%~1" -r %2 -o %3 %4 %5 %6 %7 %8 %9
