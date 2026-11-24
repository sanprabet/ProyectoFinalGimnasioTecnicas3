#!/bin/sh
set -eu

cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 21 -encoding UTF-8 -Xlint:all -d build/classes src/gimnasio/*.java
jar --create --file build/gimnasio.jar --main-class gimnasio.GymApplication -C build/classes .
