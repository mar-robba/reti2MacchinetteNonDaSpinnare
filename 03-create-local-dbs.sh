#!/bin/bash
# ============================================
# Smart Feeder — Creazione DB locali SQLite
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  💾 Smart Feeder — Creazione DB locali"
echo "=========================================="

# ID dei distributori presenti nel seed MySQL (init.sql)
DISTRIBUTORI=(1 2 3)

JAR="db-local-creator/target/db-local-creator-1.0.0.jar"

if [ ! -f "$JAR" ]; then
    echo "❌ JAR non trovato: $JAR"
    echo "   Esegui prima: ./02-build-java.sh"
    exit 1
fi

for ID in "${DISTRIBUTORI[@]}"; do
    echo "📦 Creazione distributore_${ID}.db ..."
    java -cp "db-local-creator/target/db-local-creator-1.0.0.jar:db-local-creator/target/dependency/*" com.smartfeeder.db.DBLocalCreator "$ID"
done

echo ""
echo "=========================================="
echo "  ✅ DB locali creati:"
for ID in "${DISTRIBUTORI[@]}"; do
    echo "     distributore_${ID}.db"
done
echo "=========================================="
