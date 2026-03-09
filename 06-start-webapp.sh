#!/bin/bash
# ============================================
# Smart Feeder — Avvio Web App ASP.NET
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/webapp/SmartFeederWebApp"

echo "=========================================="
echo "  🌍 Smart Feeder — Web App (Razor Pages)"
echo "=========================================="

# Controlla .NET
if ! command -v dotnet &> /dev/null; then
    echo "❌ .NET SDK non trovato. Installa .NET 8+ e riprova."
    exit 1
fi

echo "📦 Ripristino dipendenze..."
dotnet restore

echo "🚀 Avvio Web App..."
echo "   URL: http://localhost:5000"
echo "   (Ctrl+C per fermare)"
echo ""

dotnet run --urls "http://localhost:5000"
