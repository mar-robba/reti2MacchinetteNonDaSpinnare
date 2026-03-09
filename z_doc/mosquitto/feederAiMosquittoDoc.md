# Documentazione Tecnica: Broker MQTT (Eclipse Mosquitto)

## 1. Introduzione
All'interno del sistema **SmartFeeder**, la comunicazione asincrona, leggera e in tempo reale tra i dispositivi di campo (i distributori simulati "edge") e i servizi centrali (come il Server REST o l'interfaccia di Assistenza) è gestita tramite un **Broker MQTT**.
La scelta architetturale è ricaduta su **Eclipse Mosquitto**, un broker message open-source altamente portabile e leggero, che implementa pienamente le specifiche del protocollo MQTT.

---

## 2. Architettura e Deploy
Mosquitto viene eseguito come servizio containerizzato tramite **Docker** e la sua orchestrazione è definita nel file `docker-compose.yml` principale del progetto.

### Parametri del Container
- **Immagine Base**: `eclipse-mosquitto:2` (versione più recente della release 2.x).
- **Nome Container**: `smartfeeder-mosquitto`.
- **Rete Docker**: Collegato alla rete custom `smartfeeder-net` per permettere la risoluzione DNS interna (es. permettendo al Server REST o ai servizi Edge di contattarlo usando l'hostname `smartfeeder-mosquitto` all'interno della rete Docker).
- **Porte Esposte**: La porta `1883` (la porta standard non crittografata di MQTT) è mappata sulla `1883` dell'host, permettendo connessioni sia da container interni sia da applicativi esterni in fase di sviluppo/test.

### Volumi e Persistenza
Per garantire che i dati e le configurazioni non vadano persi alla distruzione del container, sono stati definiti i seguenti mapping:
1. **Configurazione Principale**: `./mosquitto/config/mosquitto.conf` viene montato in `/mosquitto/config/mosquitto.conf`.
2. **File delle Credenziali**: `./mosquitto/config/passwd` viene montato in `/mosquitto/config/passwd`.
3. **Volume Dati**: Il volume nominato Docker locale `mosquitto_data` è mappato su `/mosquitto/data`, per rendere persistenti eventuali messaggi con QoS > 0 e iscrizioni salvate.

---

## 3. Configurazione del Broker (`mosquitto.conf`)
Il comportamento del broker è imposto dal file di configurazione reso estremamente compatto e focalizzato sulla **sicurezza**:

```conf
listener 1883
allow_anonymous false
password_file /mosquitto/config/passwd
```

- **`listener 1883`**: A partire dalla versione 2.0 di Mosquitto, il demone per impostazione predefinita ascolta solo su `localhost`. Specificando `listener 1883`, si istruisce il broker a mettersi in ascolto su tutte le interfacce di rete del container per poter accettare connessioni dall'esterno.
- **`allow_anonymous false`**: Requisito fondamentale di sicurezza. Vieta le disconnessioni ai client che non forniscono un payload di autenticazione valido.
- **`password_file`**: Istruisce Mosquitto su dove reperire il dizionario di credenziali autorizzate, montato tramite Docker.

---

## 4. Autenticazione e Sicurezza
La sicurezza è un pilastro in contesti IoT dove falsi messaggi potrebbero alterare il dominio funzionale dei macchinari fisici. A tal scopo, la configurazione attuale prevede:

- **Utente Unico di Servizio**: Attualmente definito nel file `.passwd`, è presente una singola utenza globale.
  - **Username**: `smartfeeder`
  - **Password**: Il file conserva una password generata sotto forma di hash cifrato ad alta resistenza (es. `$7$...`), crittografata dall'utility `mosquitto_passwd`. Nessun utente esterno è a conoscenza della password in chiaro guardando esclusivamente il file di configurazione.

Pertanto, tutti i componenti dell'ecosistema (es. il `ServerRestApplication`, i processi in Node.js/Java dei distributori e i pannelli operativi) **devono** presentare queste credenziali in fase iniziale di handshake MQTT.

---

## 5. Ruolo nel Sistema SmartFeeder
Sebbene il broker sia un servizio infrastrutturale trasparente a livello di logica di business, il suo corretto posizionamento permette di attuare due stream funzionali cruciali:
1. **Data Telemetry (Heartbeat)**: I distributori inviano messaggi di "PING" a scadenze costanti per certificare il proprio status di connessione.
2. **Event-Driven Alerts (Ticket Guasti e Sblocchi)**: I distributori utilizzano Mosquitto per far pervenire gli allarmi al Server (es. "mangime esaurito"). Analogamente, il Server utilizza il broker per eseguire la push inversa su specifici topic (es. `MSG_RIPARAZIONE_EFFETTUATA`) e "risvegliare/sbloccare" la macchina senza che quest'ultima continui a fare polling costante su HTTP.
