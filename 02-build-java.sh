#!/bin/bash
# ============================================
# Smart Feeder — Build di tutti i moduli Java
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  🔨 Smart Feeder — Build Maven"
echo "=========================================="

# Controlla Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven non trovato. Installa Maven e riprova."
    exit 1
fi

# Controlla Java
if ! command -v java &> /dev/null; then
    echo "❌ Java non trovato. Installa Java 21+ e riprova."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1)
echo "📌 Versione Java: $JAVA_VER"

# Build Maven
echo "🔨 Build di tutti i moduli..."
mvn clean package -DskipTests

echo ""
echo "=========================================="
echo "  ✅ Build completata!"
echo "  JAR disponibili in ogni modulo/target/"
echo "=========================================="
