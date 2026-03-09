#!/bin/bash
# ============================================
# Smart Feeder — Avvio Distributore Edge
# Avvia tutti i 4 microservizi per un distributore
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

ID_DISTRIBUTORE=${1:-1}
MQTT_PASSWORD=${2:-smartfeeder123}

echo "=========================================="
echo "  📦 Smart Feeder — Distributore #$ID_DISTRIBUTORE"
echo "=========================================="

# Verifica che i JAR esistano
for MODULE in feeder-cassa feeder-erogatore feeder-assistenza feeder-ui; do
    JAR="${MODULE}/target/${MODULE}-1.0.0.jar"
    if [ ! -f "$JAR" ]; then
        echo "❌ JAR non trovato: $JAR"
        echo "   Esegui prima: ./02-build-java.sh"
        exit 1
    fi
done

# Verifica DB locale
if [ ! -f "distributore_${ID_DISTRIBUTORE}.db" ]; then
    echo "⚠️  DB locale non trovato. Creo distributore_${ID_DISTRIBUTORE}.db ..."
    java -jar db-local-creator/target/db-local-creator-1.0.0.jar "$ID_DISTRIBUTORE"
fi

echo "🚀 Avvio microservizi per distributore #$ID_DISTRIBUTORE..."
echo ""

# Funzione per gestire cleanup
cleanup() {
    echo ""
    echo "🛑 Arresto microservizi..."
    kill $PID_CASSA $PID_EROGATORE $PID_ASSISTENZA $PID_UI 2>/dev/null
    wait 2>/dev/null
    echo "✅ Tutti i microservizi arrestati."
}
trap cleanup EXIT INT TERM

# Classpath comune (per le dipendenze nel fat JAR del common)
CP_COMMON="feeder-common/target/feeder-common-1.0.0.jar"

# Avvia Cassa (background)
echo "   ➡️  Avvio Cassa..."
java -cp "feeder-cassa/target/feeder-cassa-1.0.0.jar:feeder-cassa/target/dependency/*:$CP_COMMON" \
    com.smartfeeder.cassa.CassaMain "$ID_DISTRIBUTORE" "$MQTT_PASSWORD" &
PID_CASSA=$!

sleep 1

# Avvia Erogatore (background)
echo "   ➡️  Avvio Erogatore..."
java -cp "feeder-erogatore/target/feeder-erogatore-1.0.0.jar:feeder-erogatore/target/dependency/*:$CP_COMMON" \
    com.smartfeeder.erogatore.ErogatoreMain "$ID_DISTRIBUTORE" "$MQTT_PASSWORD" &
PID_EROGATORE=$!

sleep 1

# Avvia Assistenza (background)
echo "   ➡️  Avvio Assistenza..."
java -cp "feeder-assistenza/target/feeder-assistenza-1.0.0.jar:feeder-assistenza/target/dependency/*:$CP_COMMON" \
    com.smartfeeder.assistenza.AssistenzaMain "$ID_DISTRIBUTORE" "$MQTT_PASSWORD" &
PID_ASSISTENZA=$!

sleep 1

# Avvia Interfaccia Utente (foreground con GUI)
echo "   ➡️  Avvio InterfacciaUtente (GUI)..."
java -cp "feeder-ui/target/feeder-ui-1.0.0.jar:feeder-ui/target/dependency/*:$CP_COMMON" \
    com.smartfeeder.ui.InterfacciaUtenteMain "$ID_DISTRIBUTORE" "$MQTT_PASSWORD" &
PID_UI=$!

echo ""
echo "=========================================="
echo "  ✅ Distributore #$ID_DISTRIBUTORE avviato!"
echo "  PID Cassa:       $PID_CASSA"
echo "  PID Erogatore:   $PID_EROGATORE"
echo "  PID Assistenza:  $PID_ASSISTENZA"
echo "  PID UI:          $PID_UI"
echo ""
echo "  Premi Ctrl+C per arrestare tutti."
echo "=========================================="

# Attendi che un qualsiasi processo termini
wait
