#!/usr/bin/env bash
set -euo pipefail

children=()
stop_children() {
    if (( ${#children[@]} > 0 )); then
        kill "${children[@]}" 2>/dev/null || true
        wait
    fi
}
trap stop_children EXIT
trap 'exit 0' TERM INT

Xvfb "$DISPLAY" -screen 0 1280x800x24 -nolisten tcp &
children+=("$!")
for attempt in {1..50}; do
    if xdpyinfo >/dev/null 2>&1; then
        break
    fi
    sleep 0.1
done
if ! xdpyinfo >/dev/null 2>&1; then
    echo "No se pudo iniciar la pantalla virtual." >&2
    exit 1
fi

openbox &
children+=("$!")
x11vnc -display "$DISPLAY" -localhost -rfbport 5900 -nopw -forever -shared -repeat -noxdamage &
children+=("$!")
websockify --web /usr/share/novnc 6080 127.0.0.1:5900 &
children+=("$!")
java -jar /app/gimnasio.jar &
children+=("$!")

wait -n
