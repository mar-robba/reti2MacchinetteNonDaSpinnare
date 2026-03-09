#!/bin/bash
# ============================================
# Smart Feeder — Arresto completo
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  🛑 Smart Feeder — Arresto completo"
echo "=========================================="

# Ferma container Docker
echo "🐳 Arresto container Docker..."
docker compose down 2>/dev/null || true

# Ferma processi Java Smart Feeder
echo "☕ Arresto processi Java (Smart Feeder)..."
pkill -f "com.smartfeeder" 2>/dev/null || true

# Ferma processo .NET
echo "🌍 Arresto Web App .NET..."
pkill -f "SmartFeederWebApp" 2>/dev/null || true

echo ""
echo "=========================================="
echo "  ✅ Tutto arrestato!"
echo "=========================================="
