#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <hostname> [port]"
    exit 1
fi

HOST="$1"
PORT="${2:-443}"

TMP_CHAIN="$(mktemp)"
TMP_DIR="$(mktemp -d)"

echo "[1] Fetching certificate chain from $HOST:$PORT..."

openssl s_client -showcerts -connect "$HOST:$PORT" </dev/null 2>/dev/null \
  | sed -n '/BEGIN CERTIFICATE/,/END CERTIFICATE/p' \
  | tee "$TMP_CHAIN" >/dev/null

if [ ! -s "$TMP_CHAIN" ]; then
    echo "Failed to retrieve certificate chain."
    exit 1
fi

echo "[2] Splitting chain into individual certificates..."

awk '
/BEGIN CERTIFICATE/{n++}
{print > ("'"$TMP_DIR"'/ca-" n ".crt")}
' "$TMP_CHAIN"

CERT_FILES=( "$TMP_DIR"/ca-*.crt )
LAST_CERT="${CERT_FILES[-1]}"

if [ ! -f "$LAST_CERT" ]; then
    echo "No certificates extracted."
    exit 1
fi

TARGET="/usr/local/share/ca-certificates/${HOST}-custom-ca.crt"

echo "[3] Installing CA certificate:"
echo "    $LAST_CERT → $TARGET"

cp "$LAST_CERT" "$TARGET"

echo "[4] Updating system CA store..."

update-ca-certificates

echo ""
echo "Done."
echo "Restart any affected services to pick up the new trust store. (e.g. systemctl restart docker)"

# Cleanup
rm -rf "$TMP_CHAIN" "$TMP_DIR"
