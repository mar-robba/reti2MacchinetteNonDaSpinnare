#!/bin/bash
# ============================================
# Smart Feeder — Avvio Server REST (SparkJava)
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  🌐 Smart Feeder — Server REST"
echo "=========================================="

JAR="server-rest/target/server-rest-1.0.0.jar"

if [ ! -f "$JAR" ]; then
    echo "❌ JAR non trovato: $JAR"
    echo "   Esegui prima: ./02-build-java.sh"
    exit 1
fi

PORTA=${1:-8081}

echo "🚀 Avvio ServerREST su porta $PORTA ..."
echo "   (Ctrl+C per fermare)"
echo ""

java -jar "$JAR" "$PORTA"
