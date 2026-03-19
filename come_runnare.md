# Come Runnare il Sistema PISSIR

Guida completa per compilare, configurare e avviare tutti i componenti del sistema distributore automatico.

---

## Prerequisiti

| Software | Versione | Scopo |
|----------|----------|-------|
| **JDK** | 22+ | Compilazione macchinetta e server REST |
| **Maven** | 3.9+ | Build system Java |
| **.NET SDK** | 8.0+ | Web App ASP.NET |
| **Docker** + **Docker Compose** | Recente | Keycloak, MySQL, Mosquitto |

---

## 1. Avvio Infrastruttura (Docker)

Dalla directory radice del progetto:

```bash
# Avvia MySQL, Mosquitto e Keycloak
docker compose up -d

# Verifica che i container siano attivi
docker compose ps
```

Servizi esposti:
- **MySQL**: `localhost:3306` (user: `pissir_user`, password: `pissir_password`, db: `pissir_db`)
- **Mosquitto**: `localhost:1883` (MQTT), `localhost:9001` (WebSocket)
- **Keycloak**: `http://localhost:8080` (admin/admin)

### Configurazione password Mosquitto

```bash
# Genera il file password criptato
docker exec -it pissir-mosquitto mosquitto_passwd -c /mosquitto/config/password.txt macchinetta1
# Inserire: password1

docker exec -it pissir-mosquitto mosquitto_passwd /mosquitto/config/password.txt macchinetta2
# Inserire: password2

docker exec -it pissir-mosquitto mosquitto_passwd /mosquitto/config/password.txt server
# Inserire: serverpass

# Riavvia Mosquitto per applicare
docker compose restart mosquitto
```

---

## 2. Compilazione e Avvio Server REST

```bash
cd server-rest

# Compilazione
mvn clean compile

# Avvio (porta 8081)
mvn exec:java -Dexec.mainClass="server.ServerREST"
```

Verifica:
```bash
curl http://localhost:8081/api/info
# Risposta attesa: {"nome":"Server REST PISSIR","versione":"1.0","porta":8081}

curl http://localhost:8081/api/scuole
# Risposta attesa: lista delle scuole dal DB
```

---

## 3. Compilazione e Avvio Macchinetta (4 microservizi)

```bash
cd macchinetta

# Compilazione
mvn clean compile
```

Ogni microservizio va avviato in un **terminale separato**. L'ordine consigliato è:

### Terminale 1: Assistenza
```bash
mvn exec:java -Dexec.mainClass="microservizi.assistenza.Assistenza" -Dexec.args="1 password1"
```

### Terminale 2: Erogatore
```bash
mvn exec:java -Dexec.mainClass="microservizi.erogatore.ErogatoreMain" -Dexec.args="1 password1 dev"
```

### Terminale 3: Cassa
```bash
mvn exec:java -Dexec.mainClass="microservizi.cassa.CassaMain" -Dexec.args="1 password1 dev"
```

### Terminale 4: Interfaccia Utente (con GUI)
```bash
mvn exec:java -Dexec.mainClass="microservizi.interfacciautente.Interfacciautente" -Dexec.args="1 password1 dev"
```

> **Nota**: il primo argomento (`1`) è l'ID della macchinetta. Per una seconda macchinetta, usare `2` e `password2`.

---

## 4. Avvio Web App (ASP.NET)

```bash
cd webapp

# Ripristina dipendenze
dotnet restore

# Avvio in modalità sviluppo (porta 5000/5001)
dotnet run
```

Accedi a: **http://localhost:5000** (o la porta indicata nel terminale).

---

## 5. Ordine di avvio completo consigliato

1. `docker compose up -d` (Infrastruttura)
2. Configurare password Mosquitto (solo la prima volta)
3. Avviare il **Server REST** (terminale dedicato)
4. Avviare i **4 microservizi** della macchinetta (4 terminali)
5. Avviare la **Web App** (terminale dedicato)

---

## 6. Arresto del sistema

```bash
# Arrestare i processi Java con Ctrl+C nei rispettivi terminali
# Arrestare la Web App con Ctrl+C

# Spegnere l'infrastruttura Docker
docker compose down

# Per eliminare anche i volumi (dati persistenti):
docker compose down -v
```

---

## Troubleshooting

| Problema | Soluzione |
|----------|-----------|
| `BindException: Indirizzo già in uso` | Un altro processo usa la porta. Trova e termina: `lsof -i :8081` |
| `Connection refused` su MQTT | Verificare che Mosquitto sia attivo: `docker compose ps` |
| `Access denied` su MySQL | Verificare credenziali in `docker-compose.yml` |
| GUI non si apre | Verificare che sia disponibile un display (X11 o Wayland) |
| Errore Keycloak al primo avvio | Attendere ~30 secondi per l'inizializzazione del container |
