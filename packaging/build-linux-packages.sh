#!/bin/sh
# Builds the Linux packages, each one in the container of its distribution (needs Docker):
#   ubuntu 24.04  -> dist/linux/ubuntu/*.deb
#   debian 13     -> dist/linux/debian/*.deb
#   fedora        -> dist/linux/fedora/*.rpm
#   arch          -> dist/linux/arch/*.pkg.tar.zst
#
# Usage: packaging/build-linux-packages.sh [ubuntu] [debian] [fedora] [arch]   (default: all four)

set -e
cd "$(dirname "$0")/.."
command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }
[ $# -gt 0 ] || set -- ubuntu debian fedora arch
# Created here, so the host user owns it (the containers write into it)
mkdir -p dist/linux

for distro in "$@"; do
	case "$distro" in
	ubuntu) IMAGE=ubuntu:24.04 ;;
	debian) IMAGE=debian:trixie ;;
	fedora) IMAGE=fedora:latest ;;
	arch) IMAGE=archlinux:latest ;;
	*)
		echo "Unknown distribution: $distro (use ubuntu, debian, fedora or arch)" >&2
		exit 1
		;;
	esac
	echo "== $distro ($IMAGE)"
	docker run --rm -v "$PWD:/src" -w /src \
		-e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" -e DIST_DIR="dist/linux/$distro" \
		"$IMAGE" sh packaging/linux-container.sh "$distro"
done
echo "Packages in dist/linux/"
