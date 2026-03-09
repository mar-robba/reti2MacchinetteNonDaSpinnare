#!/bin/bash
# ============================================
# Smart Feeder — Avvio Infrastruttura Docker
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  🐦 Smart Feeder — Infrastruttura"
echo "=========================================="

# Controlla Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker non trovato. Installa Docker e riprova."
    exit 1
fi

# Avvia i container
echo "🚀 Avvio container Docker (MySQL, Mosquitto, Keycloak)..."
docker compose up -d

# Attendi MySQL
echo "⏳ Attesa avvio MySQL..."
for i in {1..30}; do
    if docker exec smartfeeder-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        echo "✅ MySQL pronto!"
        break
    fi
    sleep 2
    echo "   ancora in attesa... ($i/30)"
done

# Attendi Keycloak
echo "⏳ Attesa avvio Keycloak (può richiedere ~60s)..."
for i in {1..40}; do
    if curl -s http://localhost:9000/health/ready 2>/dev/null | grep -q "UP"; then
        echo "✅ Keycloak pronto!"
        break
    fi
    sleep 3
    echo "   ancora in attesa... ($i/40)"
done

echo ""
echo "=========================================="
echo "  ✅ Infrastruttura avviata!"
echo "  MySQL:     localhost:3306"
echo "  Mosquitto: localhost:1883"
echo "  Keycloak:  http://localhost:8080"
echo "    Admin:   admin / admin"
echo "=========================================="
