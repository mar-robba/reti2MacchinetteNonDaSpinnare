#!/bin/bash

# ============================================================
#  run_microservizi.sh
#  Compila (mvn package) e avvia i 4 microservizi in processi
#  separati, ognuno con il proprio file di log.
# ============================================================

set -e

# ---------- Colori ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ---------- Parametri (modificabili) ----------
ID="1"
PASSWORD="password1"
ENV="dev"
LOG_DIR="./logs"

# ---------- Pulizia al Ctrl+C ----------
PIDS=()
cleanup() {
    echo -e "\n${YELLOW}[STOP] Interruzione rilevata — termino i microservizi...${NC}"
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            echo -e "  ${RED}✗${NC} PID $pid terminato"
        fi
    done
    echo -e "${GREEN}[DONE] Tutti i processi fermati.${NC}"
    exit 0
}
trap cleanup SIGINT SIGTERM

# ---------- Setup ----------
mkdir -p "$LOG_DIR"

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  Build & Run — 4 Microservizi${NC}"
echo -e "${CYAN}============================================${NC}"

# ---------- 1. Build ----------
echo -e "\n${YELLOW}[BUILD] Compilazione con Maven...${NC}"
mvn package -DskipTests -q
echo -e "${GREEN}[BUILD] Completata.${NC}"

# ---------- 2. Avvio microservizi ----------
start_service() {
    local name="$1"
    local main_class="$2"
    local args="$3"
    local log="$LOG_DIR/${name}.log"

    echo -e "\n${CYAN}[START]${NC} $name"
    echo -e "        Main  : $main_class"
    echo -e "        Args  : $args"
    echo -e "        Log   : $log"

    mvn exec:java \
        -Dexec.mainClass="$main_class" \
        -Dexec.args="$args" \
        > "$log" 2>&1 &

    local pid=$!
    PIDS+=("$pid")
    echo -e "        ${GREEN}PID $pid avviato${NC}"
}

start_service "assistenza"        "microservizi.assistenza.Assistenza"                  "$ID $PASSWORD"
start_service "erogatore"         "microservizi.erogatore.ErogatoreMain"                "$ID $PASSWORD $ENV"
start_service "cassa"             "microservizi.cassa.CassaMain"                        "$ID $PASSWORD $ENV"
start_service "interfacciautente" "microservizi.interfacciautente.Interfacciautente"    "$ID $PASSWORD $ENV"

# ---------- 3. Riepilogo ----------
echo -e "\n${CYAN}============================================${NC}"
echo -e "${GREEN}  Tutti e 4 i microservizi sono in esecuzione${NC}"
echo -e "${CYAN}============================================${NC}"
echo -e "  Log salvati in: ${YELLOW}${LOG_DIR}/${NC}"
echo -e "  Premi ${RED}Ctrl+C${NC} per fermare tutto.\n"

# ---------- 4. Attendi la fine di tutti i processi ----------
wait "${PIDS[@]}"
