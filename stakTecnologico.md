# Riassunto delle Tecnologie del Progetto PISSIR

Di seguito è riportato un elenco delle tecnologie, framework e strumenti utilizzati nel progetto, suddivisi per area di applicazione, con una descrizione del loro scopo operativo.

## Linguaggi di Programmazione e Framework
* **Java**: Utilizzato come ambiente di sviluppo di base per l'implementazione del server e dei vari microservizi.
* **ASP.NET (C#) con RazorPages**: Impiegato per lo sviluppo dell'applicazione web lato front-end destinata alla parte gestionale e amministrativa.
* **Apache Spark**: Framework utilizzato per costruire il backend RESTful della parte gestionale, che si occupa dell'elaborazione e della gestione dei dati interagendo con il database.

## Database e Gestione Dati
* **MySQL**: Scelto come database management system per l'infrastruttura centralizzata della parte gestionale.
* **SQLite**: Utilizzato per la connessione e l'interazione con i database locali ospitati in ogni singola macchinetta.
* **JSON**: Formato impiegato per gestire i file dei database locali delle singole macchinette e per la sincronizzazione dei dati con il server.

## Comunicazione e Messaggistica
* **Mosquitto (Protocollo MQTT)**: Utilizzato come broker di messaggistica per abilitare la comunicazione tra i microservizi interni alla macchinetta (Cassa, Erogatore, Assistenza, Interfaccia Utente) e per metterli in contatto con il Server REST.
* **HTTPS e SSL/TLS**: Protocolli implementati per crittografare la comunicazione tra client e server nell'applicazione web, assicurando autenticità del server e protezione contro accessi non autorizzati.

## Autenticazione, Infrastruttura e Strumenti di Build
* **Keycloak**: Servizio utilizzato per gestire in sicurezza l'autenticazione, l'autorizzazione e la distinzione dei ruoli (amministratore, impiegato, tecnico) all'interno dell'applicazione web.
* **Docker e Docker Desktop**: Strumento di containerizzazione impiegato in particolare per la configurazione, l'avvio e la gestione dell'ambiente Keycloak.
* **Maven**: Strumento di build automation utilizzato per la creazione, la configurazione e la gestione delle dipendenze del progetto.

## Testing e Ambienti di Sviluppo
* **JUnit**: Framework impiegato per scrivere ed eseguire i test di verifica e validazione sui microservizi.
* **NetBeans**: Ambiente di sviluppo utilizzato specificamente per l'implementazione e la personalizzazione grafica dell'interfaccia utente della macchinetta.
* **Eclipse**: Ambiente di sviluppo per l'esecuzione del Server REST e la gestione dei progetti Maven legati alla macchinetta.
* **IntelliJ IDEA**: Utilizzato per sfruttare le configurazioni che permettono di avviare simultaneamente i vari microservizi che compongono il sistema.
