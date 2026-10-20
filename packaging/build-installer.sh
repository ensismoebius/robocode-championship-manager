#!/bin/sh
# Builds the installer of the system it runs on, into dist/ (or $DIST_DIR):
#   Ubuntu, Debian -> .deb             Fedora -> .rpm            Arch -> .pkg.tar.zst
#   macOS          -> .dmg             any    -> app-image (a folder, to try without installing)
#
# Usage: packaging/build-installer.sh [deb|rpm|arch|dmg|app-image]
# The type defaults to the distribution of the system.
# Needs JDK 21 (jpackage, jlink, jdeps) and Maven. Arch needs makepkg, run as a normal user.
# jpackage can not cross-build: run it on each system, or use build-linux-packages.sh (Docker).
# On Windows use build-installer.ps1.

set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
DIST_DIR=${DIST_DIR:-dist}

case "$(uname -s)" in
Linux)
	[ -f /etc/os-release ] && . /etc/os-release
	case " ${ID:-} ${ID_LIKE:-} " in
	*" arch "*) DEFAULT_TYPE=arch ;;
	*" fedora "* | *" rhel "*) DEFAULT_TYPE=rpm ;;
	*) DEFAULT_TYPE=deb ;;
	esac
	;;
Darwin) DEFAULT_TYPE=dmg ;;
*)
	echo "Unsupported system: $(uname -s). On Windows run packaging/build-installer.ps1" >&2
	exit 1
	;;
esac
TYPE=${1:-$DEFAULT_TYPE}

case "$TYPE" in
deb | rpm | arch | dmg | app-image) ;;
*)
	echo "Unknown type: $TYPE (use deb, rpm, arch, dmg or app-image)" >&2
	exit 1
	;;
esac

for tool in java jpackage jlink jdeps mvn; do
	command -v "$tool" >/dev/null 2>&1 || { echo "$tool not found, install a JDK 21 and Maven" >&2; exit 1; }
done
if [ "$TYPE" = arch ]; then
	command -v makepkg >/dev/null 2>&1 || { echo "makepkg not found (Arch Linux)" >&2; exit 1; }
	[ "$(id -u)" -ne 0 ] || { echo "makepkg does not run as root: run this as a normal user" >&2; exit 1; }
fi

VERSION=$(sed -n 's:^\t<version>\(.*\)</version>$:\1:p' pom.xml | head -1)
JAR=target/RobocodeChampManager-$VERSION-all.jar

echo "Building $JAR..."
mvn -B -q -DskipTests package

# The modules the program needs, found by jdeps, plus the ones it loads by reflection
MODULES=$( (jdeps --print-module-deps --ignore-missing-deps "$JAR"; echo "java.desktop,java.naming,java.management,java.sql,java.xml,java.prefs,java.logging,java.net.http,jdk.crypto.ec,jdk.unsupported") | tr ',' '\n' | sed '/^$/d' | sort -u | paste -sd, -)

STAGE=packaging/build/input
RUNTIME=packaging/build/runtime
APPS=packaging/build/app
rm -rf packaging/build "$DIST_DIR"
mkdir -p "$STAGE" "$DIST_DIR"
cp "$JAR" "$STAGE/RobocodeChampManager.jar"

# A runtime with the java launcher too: the battles run java from JAVA_HOME (see RobocodeStarter/start.sh)
jlink --add-modules "$MODULES" --strip-debug --no-header-files --no-man-pages --output "$RUNTIME"

case "$TYPE" in
dmg) ICON=packaging/icons/app.icns ;;
*) ICON=packaging/icons/app.png ;;
esac

# Arch: the folder of the program is packaged by makepkg, below
JP_TYPE=$TYPE
OUT=$DIST_DIR
if [ "$TYPE" = arch ]; then
	JP_TYPE=app-image
	OUT=$APPS
	mkdir -p "$APPS"
fi

set -- --type "$JP_TYPE" \
	--name RobocodeChampManager \
	--app-version "$VERSION" \
	--vendor "André Furlan" \
	--description "Gerenciador de campeonatos de robocode" \
	--input "$STAGE" \
	--main-jar RobocodeChampManager.jar \
	--main-class Launcher \
	--runtime-image "$RUNTIME" \
	--app-content RobocodeStarter \
	--icon "$ICON" \
	--dest "$OUT"
case "$TYPE" in
deb)
	set -- "$@" --linux-shortcut --linux-app-category games \
		--linux-package-deps "libgtk-3-0t64 | libgtk-3-0, libasound2t64 | libasound2" \
		--linux-deb-maintainer "André Furlan <ensismoebius@gmail.com>"
	;;
rpm)
	set -- "$@" --linux-shortcut --linux-app-category games --linux-package-deps "gtk3, alsa-lib"
	;;
esac
jpackage "$@"

if [ "$TYPE" = arch ]; then
	WORK=packaging/build/arch
	mkdir -p "$WORK"
	cp packaging/linux/robocodechampmanager.desktop "$WORK/"
	cat > "$WORK/PKGBUILD" <<PKGBUILD
pkgname=robocodechampmanager
pkgver=$VERSION
pkgrel=1
pkgdesc='Gerenciador de campeonatos de robocode'
arch=('$(uname -m)')
url='https://sourceforge.net/projects/robocode-championship-manager/'
license=('GPL-3.0-only')
depends=('gtk3' 'alsa-lib')
options=('!strip' '!debug')

package() {
	install -d "\$pkgdir/opt/\$pkgname"
	cp -a "$ROOT/$APPS/RobocodeChampManager/." "\$pkgdir/opt/\$pkgname/"
	install -d "\$pkgdir/usr/bin"
	ln -s "/opt/\$pkgname/bin/RobocodeChampManager" "\$pkgdir/usr/bin/\$pkgname"
	install -Dm644 "$ROOT/packaging/linux/robocodechampmanager.desktop" "\$pkgdir/usr/share/applications/\$pkgname.desktop"
	install -Dm644 "$ROOT/packaging/icons/app.png" "\$pkgdir/usr/share/icons/hicolor/512x512/apps/\$pkgname.png"
}
PKGBUILD
	(cd "$WORK" && makepkg -f -d)
	cp "$WORK"/*.pkg.tar.zst "$DIST_DIR/"
fi

echo "Done. Output in $DIST_DIR:"
ls -la "$DIST_DIR"
