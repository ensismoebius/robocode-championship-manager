#!/bin/sh
# Runs inside a distribution's container (see build-linux-packages.sh): installs the tools
# of that distribution, builds its package and gives the files back to the host user.
# Usage (in the container): sh packaging/linux-container.sh ubuntu|debian|fedora|arch
# Environment: HOST_UID, HOST_GID, DIST_DIR

set -e
DISTRO=$1
cd /src
trap 'chown -R "$HOST_UID:$HOST_GID" target packaging/build "$DIST_DIR" 2>/dev/null || true' EXIT

case "$DISTRO" in
ubuntu | debian)
	apt-get update -qq
	DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jdk-headless maven
	sh packaging/build-installer.sh deb
	;;
fedora)
	dnf install -y -q java-21-openjdk-devel java-21-openjdk-jmods maven rpm-build
	sh packaging/build-installer.sh rpm
	;;
arch)
	pacman-key --init >/dev/null 2>&1
	pacman-key --populate archlinux >/dev/null 2>&1
	pacman -Sy --noconfirm --needed jdk21-openjdk maven base-devel
	# makepkg does not run as root
	useradd -m -u "$HOST_UID" builder
	runuser -u builder -- env HOME=/home/builder DIST_DIR="$DIST_DIR" sh packaging/build-installer.sh arch
	;;
*)
	echo "Unknown distribution: $DISTRO" >&2
	exit 1
	;;
esac
