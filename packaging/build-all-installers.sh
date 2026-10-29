#!/bin/sh
# Builds every installer of the program into dist/, from Linux:
#   linux    the packages of Ubuntu, Debian, Fedora and Arch, each in the container of its distribution (needs Docker)
#   windows  the .exe, built in the Windows of WinBoat (the docker container "WinBoat", started before)
#
# Usage: packaging/build-all-installers.sh [linux] [windows]   (default: both)
#
# Windows: WinBoat does not run commands for us, so this script writes the build as a batch file in the folder
# ~/.cache/robocode-champ-installers, which WinBoat shares with Windows (the whole home is \\host.lan\Data), and waits
# for you to open it in Windows (Shared > .cache > robocode-champ-installers > windows-build.bat, one double click).
# The batch installs JDK 21, Maven and WiX Toolset 3 in C:\rcm-tools if they are missing (packaging/setup-windows-tools.ps1),
# copies the sources to C:\rcm-build, builds there, so target/ and dist/ of this folder are not touched, and brings
# only the .exe back to dist/. The project must be under $HOME.
# Environment: WINBOAT (the name of the container, WinBoat), WINDOWS_TIMEOUT (the minutes to wait for the build, 60).

set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
WINBOAT=${WINBOAT:-WinBoat}
WINDOWS_TIMEOUT=${WINDOWS_TIMEOUT:-60}
[ $# -gt 0 ] || set -- linux windows

build_linux() {
	echo "== Linux packages"
	packaging/build-linux-packages.sh
}

build_windows() {
	echo "== Windows installer (in the container $WINBOAT)"
	command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }
	[ "$(docker inspect -f '{{.State.Running}}' "$WINBOAT" 2>/dev/null)" = true ] || { echo "the container $WINBOAT is not running: start Windows in WinBoat" >&2; exit 1; }
	case "$ROOT" in
	"$HOME"/*) ;;
	*)
		echo "the project must be under $HOME, the folder WinBoat shares with Windows" >&2
		exit 1
		;;
	esac

	WORK="$HOME/.cache/robocode-champ-installers"
	mkdir -p "$WORK" "$ROOT/dist"
	REL=$(printf '%s' "${ROOT#"$HOME"/}" | tr '/' '\\')
	SHARE='\\host.lan\Data'
	SRC="$SHARE\\$REL"
	WSHARE="$SHARE\\.cache\\robocode-champ-installers"

	rm -f "$WORK/windows.status" "$WORK/windows.log"
	# What Windows runs: the tools, the sources, the build and the .exe back; its state is in windows.status
	cat >"$WORK/windows-build.bat" <<EOF
@echo off
set SRC=$SRC
set WORK=C:\\rcm-build
set STATUS=$WSHARE\\windows.status
set LOG=$WSHARE\\windows.log
echo running> "%STATUS%"
echo Tools> "%LOG%"
powershell -ExecutionPolicy Bypass -File "%SRC%\\packaging\\setup-windows-tools.ps1" >> "%LOG%" 2>&1
if errorlevel 1 goto failed
set JAVA_HOME=C:\\rcm-tools\\jdk
set PATH=C:\\rcm-tools\\jdk\\bin;C:\\rcm-tools\\maven\\bin;C:\\rcm-tools\\wix;%PATH%
echo Sources>> "%LOG%"
rmdir /s /q "%WORK%" 2>nul
robocopy "%SRC%" "%WORK%" /MIR /XD "%SRC%\\target" "%SRC%\\dist" "%SRC%\\.git" "%SRC%\\packaging\\build" /NFL /NDL /NJH /NJS /NP >> "%LOG%" 2>&1
if errorlevel 8 goto failed
cd /d "%WORK%"
echo Build>> "%LOG%"
powershell -ExecutionPolicy Bypass -File packaging\\build-installer.ps1 >> "%LOG%" 2>&1
if errorlevel 1 goto failed
copy /y "%WORK%\\dist\\*.exe" "%SRC%\\dist\\" >> "%LOG%" 2>&1
if errorlevel 1 goto failed
echo ok> "%STATUS%"
exit /b 0
:failed
echo failed> "%STATUS%"
exit /b 1
EOF
	# cmd wants the lines of a batch file to end with CR LF
	sed -i 's/$/\r/' "$WORK/windows-build.bat"
	echo "In Windows, open Shared > .cache > robocode-champ-installers and double click windows-build.bat; waiting for it..."

	# The build is done when the batch writes ok or failed
	DEADLINE=$(($(date +%s) + WINDOWS_TIMEOUT * 60))
	STATE=
	LAST=
	while [ "$STATE" != ok ] && [ "$STATE" != failed ]; do
		if [ "$(date +%s)" -gt "$DEADLINE" ]; then
			echo "the Windows build took more than $WINDOWS_TIMEOUT minutes; see $WORK/windows.log" >&2
			exit 1
		fi
		sleep 5
		STATE=$(cat "$WORK/windows.status" 2>/dev/null | tr -d '\r\n ' || true)
		STEP=$(cat "$WORK/windows.log" 2>/dev/null | tr -d '\r' | grep -E '^(Tools|Sources|Build)$' | tail -1 || true)
		[ -z "$STEP" ] || [ "$STEP" = "$LAST" ] || { echo "Windows: $STEP"; LAST=$STEP; }
	done
	tr -d '\r' <"$WORK/windows.log" | tail -n 15
	if [ "$STATE" != ok ]; then
		echo "the Windows build failed; see $WORK/windows.log" >&2
		exit 1
	fi
	echo "Windows installer in dist/:"
	ls -la "$ROOT"/dist/*.exe
}

for target in "$@"; do
	case "$target" in
	linux) build_linux ;;
	windows) build_windows ;;
	*)
		echo "Unknown target: $target (use linux or windows)" >&2
		exit 1
		;;
	esac
done
echo "Done. Installers in dist/"
