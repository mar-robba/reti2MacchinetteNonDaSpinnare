# Documentazione Server REST PISSIR

Questa cartella contiene il codice sorgente per il microservizio **Server REST** del progetto PISSIR. Il server agisce da intermediario tra i client (come la Web App Gestionale), il Database centralizzato MySQL e le macchinette distribuite tramite il protocollo MQTT.

## Struttura e Architettura

Il server è scritto in **Java** e strutturato principalmente in tre classi fondamentali situate nel package `server`:

### 1. `ServerREST.java`
Questa è la classe principale (entry-point) dell'applicazione. Utilizza il framework **Spark Java** per esporre un'API RESTful in ascolto sulla porta `8081`. 
L'API viene utilizzata dalla Web App per effettuare le operazioni di CRUD (Creazione, Lettura, Aggiornamento, Eliminazione) sulle entità del sistema.

**Funzionalità principali:**
- Inizializza la connessione al database tramite `DatabaseManager`.
- Inizializza e avvia il client MQTT tramite `ServerREST_MQTT`.
- Definisce i filtri CORS (Cross-Origin Resource Sharing) per permettere alla Web App di comunicare con le API.
- Espone gli endpoint RESTful raggruppati per entità:
  - **Scuole**: Endpoint per ottenere la lista `/api/scuole`, aggiungere `/api/scuole` ed eliminare `/api/scuole/:id` gli istituti.
  - **Macchinette**: Endpoint per recuperare informazioni  (es. per scuola o stato specifico), aggiungere o eliminare macchinette e inviare esplicitamente un tecnico (`/api/macchinette/:id/invia-tecnico`).
  - **Richieste Tecnico**: Recupero ed eliminazione (ossia marcamento come completata) delle richieste inviate al personale tecnico.
- Fornisce endpoint helper per convertire le risposte in formato JSON e gestire gli errori.

### 2. `DatabaseManager.java`
Questa classe funge da **Data Access Object (DAO)** gestendo tutta l'interazione diretta con il database relazionale MySQL.
Fornisce i metodi necessari per interrogare e persistere le informazioni.

**Funzionalità principali:**
- Stabilisce e gestisce le connessioni JDBC tramite le credenziali configurate nel costruttore.
- **Scuole**: Esegue statement SQL per selezionare, inserire e cancellare scuole. Il sistema previene l'aggiunta di scuole duplicate tramite controlli preventivi sul nome.
- **Macchinette**: Fornisce query per visualizzare la lista (anche con `JOIN` per restituire il nome della scuola associata), inserimento, eliminazione e controllo dell'esistenza. Include metodi vitali per l'aggiornamento dinamico dei *Flag di stato* originati dagli allarmi inviati via MQTT.
- **Tecnici e Richieste**: Gestisce la creazione nel DB di ticket per i tecnici in caso di anomalie e la loro archiviazione una volta che l'anomalia viene considerata risolta.
- Costruisce le risposte JSON (sfruttando le librerie GSON) a partire dalle astrazioni natie di SQL (`ResultSet`).

### 3. `ServerREST_MQTT.java`
Questo componente rappresenta il vero e proprio *ponte* di comunicazione asincrona tra il server e le macchinette. Sfrutta il protocollo **MQTT** (libreria Eclipse Paho).

**Funzionalità principali:**
- Si connette al Broker MQTT (ad es. un contenitore Mosquitto in esecuzione su localhost:1883).
- **Iscrizioni (Subscribing)**: Ascolta dinamicamente i topic di broadcast associati a ciascuna macchinetta (`server/macchinetta/+/guasto`, `guastoRisolto`, `stato`).
- **Gestore Guasti**: Analizza il *payload* dei messaggi elaborati. Se viene rilevato un guasto:
  - Invoca il `DatabaseManager` per settare i vari `flag` di emergenza (cassa piena, esaurimento zucchero, ecc).
  - Richiede la generazione automatica di un nuovo ticket per l'intervento del tecnico.
- **Risoluzione Anomalie**: Ascolta sul topic `guastoRisolto` e riporta alla normalità nel database le macchinette soggette precedentemente ad allarmi.
- **Pubblicazione (Publishing)**: Offre il metodo per poter notificare direttamente la singola macchinetta in casi specifici (ex. Inoltro di un comando dal gestionale verso la macchinetta specificando che il tecnico è in arrivo).

## Dipendenze Utilizzate
L'applicativo sfrutta Maven (`pom.xml`) per gestire le divere dipendenze:
- **Spark Core**: Framework web ultra leggero e veloce per Java.
- **Eclipse Paho MQTT**: Client in Java per la comunicazione su rete MQTT.
- **Google Gson**: Libreria potente per mappare e parsare oggetti in JSON.
- **MySQL Connector/J**: Driver ufficiale per l'interazione JDBC con il database PISSIR.

## Esecuzione e Test
Una volta che l'ambiente è stato compilato e inizializzato, l'applicativo accoglie le richieste su base `http://localhost:8081/api/`. Il server ha bisogno che il Broker MQTT (porta standard 1883) e il database MySQL siano inizializzati affinché il proprio avvio non causi eccezioni per mancanza di reperibilità dei servizi di interdipendenza.
