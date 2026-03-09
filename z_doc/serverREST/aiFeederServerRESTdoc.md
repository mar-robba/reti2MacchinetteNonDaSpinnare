# Documentazione Tecnica: Server REST

## 1. Introduzione
Il componente **`server-rest`** rappresenta il cuore del backend nel sistema SmartFeeder. Sviluppato in **Java**, il suo compito principale è di fare da ponte tra i dispositivi di campo (i distributori intelligenti, simulati o fisici), il database relazionale (MySQL) e il pannello di controllo Web (Web App frontend).

Esso espone un'interfaccia **RESTful** per le operazioni CRUD necessarie alla gestione del parco macchine e, allo stesso tempo, si integra come **Client MQTT** per la comunicazione asincrona e in tempo reale con i componenti "edge" o con il nodo di Assistenza.

---

## 2. Architettura e Tecnologie
Il modulo è stato realizzato utilizzando un set di librerie snelle e performanti:
- **SparkJava**: Framework leggero per la creazione di API RESTful, configurato per rimanere in ascolto sulla porta `8081`.
- **HikariCP**: Connection pool ad alte prestazioni per la gestione ottimizzata delle connessioni al database MySQL.
- **Eclipse Paho**: Client MQTT utilizzato per la sottoscrizione ai topic del broker e la ricezione di eventi (es. guasti, heartbeat).
- **Gson (Google)**: Libreria impiegata per la serializzazione e deserializzazione automatica degli oggetti Java in formato JSON e viceversa.

L'architettura del codice segue un **pattern architetturale a strati semplificato**:
- **Entry Point (`ServerRestApplication`)**: Inizializza il DB, il demone MQTT, configura il CORS, avvia l'engine HTTP di Spark e mappa le eccezioni e le rotte non trovate in comode risposte JSON.
- **Routing (`routes/`)**: Ricevono le chiamate HTTP, estraggono i parametri/body e le smistano ai data access objects.
- **DAO (`dao/`)**: Data Access Objects che centralizzano ed eseguono le query SQL sulle varie tabelle.
- **Model (`model/`)**: Classi POJO/Entities che rappresentano in memoria il dominio (Parco, Distributore, TicketGuasto).
- **MQTT Handler (`mqtt/`)**: Listener in background che traduce gli eventi MQTT in scritture sul database (es. tracciamento dei guasti).

---

## 3. Gestione Dati ed Entità (`DatabaseManager`)
La persistenza avviene su database relazionale:
- **`DatabaseManager`**: Inizializza il DataSource specificando variabili d'ambiente (sovrascrivibili ad esempio in un ambiente Docker) quali `DB_URL` (default `jdbc:mysql://localhost:3306/SmartFeederDB`), `DB_USER` e `DB_PASSWORD`. Il dimensionamento del pool prevede un massimo di 10 connessioni contemporanee.
- I corrispondenti **DAO** (es. `DistributoreDao`, `ParcoDao`, `TicketGuastoDao`) si occupano di aprire le connessioni erogate dal manager e sottomettere PreparedStatement blindati, convertendo i `ResultSet` in liste di oggetti di dominio.

---

## 4. API Endpoints (Rotte HTTP)
Nel package `com.smartfeeder.server.routes` vengono esposti gli endpoint invocabili dalla Web App:

### API Parchi (`ParcoRoutes`)
Permettono di gestire i raggruppamenti (Parchi) di più distributori.
- **`GET /api/parchi`**: Restituisce tutti i parchi registrati nel sistema.
- **`GET /api/parchi/:id`**: Dettaglio del singolo parco cercato per ID.
- **`POST /api/parchi`**: Inserisce e restituisce un nuovo parco.
- **`DELETE /api/parchi/:id`**: Elimina un parco.

### API Distributori (`DistributoreRoutes`)
Gestione delle anagrafiche dei macchinari fisici installati nei vari Parchi.
- **`GET /api/distributori`**: Elenco globale.
- **`GET /api/distributori/:id`**: Dettaglio del singolo.
- **`GET /api/distributori/parco/:idParco`**: Tutti i macchinari appartenenti a uno specifico `idParco`.
- **`POST /api/distributori`**: Registrazione di una nuova risorsa.
- **`DELETE /api/distributori/:id`**: Eliminazione di un macchinario.

### API Ticket Guasti (`TicketRoutes`)
Monitoraggio dello stato di salute e gestione allarmi.
- **`GET /api/ticket`**: Elenca i ticket guasto. Accetta una query optionale `?stato=` (es: "?stato=aperta").
- **`GET /api/ticket/distributore/:id`**: Restituisce lo storico dei ticket per uno specifico macchinario.
- **`POST /api/ticket`**: Genera un nuovo ticket partendo da un invio HTTP (utilizzato dal nodo Assistenza in assenza del canale MQTT). Imposta lo stato del distributore su `guasto = true`.
- **`POST /api/ticket/:id/risolvi`**: Segna un ticket come "risolto". Questo rimette il distributore tra le macchine non guaste ed è responsabile per **inoltrare tramite MQTT** il segnale `MSG_RIPARAZIONE_EFFETTUATA` verso il field/macchinario originario.
- **`POST /api/inviaAllarme`**: In via eccezionale e per compatibilità con l'edge bypassando MQTT, permette la loggatura di allarmi e l'accensione immediata delle spie di guasto.

---

## 5. Integrazione MQTT (`MqttMessageHandler`)
`server-rest` non agisce solo come un server HTTP passivo ma partecipa attivamente allo stack asincrono MQTT in modalità Publisher/Subscriber.

### Ricezione (Subscriber)
All'avvio, il Server REST istanzia `MqttMessageHandler` e lo collega al broker su un thread separato (nome thread: `mqtt-handler`). 
Si iscrive sul pattern wildcard **`/smartfeeder/+/assistenza/serverREST/`** per catturare i messaggi di tutti i distributori verso lo strato server.
- **Ping/Heartbeat**: Se il payload è un ping (`MSG_PING`), aggiorna il campo database che attesta `l'Ultimo Contatto`, per determinare la raggiungibilità.
- **Guasti e Allarmi**: Se il payload è un blocco funzionale (es. cassa piena, mangime esaurito, errori di erogazione), la classe istanzia un **TicketGuasto** automatico nel DB, marchiato `"aperta"`, ed oscura lo stato del distributore a `guasta = true`.

### Trasmissione (Publisher)
Tramite il metodo esposto `inviaRiparazione(int idDistributore)` è possibile notificare ai macchinari l'avvenuto sblocco manutentivo. Lo stesso viene scaturito, come visto in precedenza in `TicketRoutes`, quando dal pannello web si imposta un ticket nello status di "risolto". Il server invia il pacchetto MQTT di sblocco e permette al microservizio edge di ripristinare il corretto funzionamento.
