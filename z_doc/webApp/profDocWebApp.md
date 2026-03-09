# Documentazione Web App (Parte Gestionale)

## 1. Introduzione
[cite_start]La parte amministrativa del progetto è stata sviluppata come applicazione web server-side[cite: 60]. 
- [cite_start]**Framework**: ASP.NET con RazorPages (C#)[cite: 60]. [cite_start]L'implementazione del front-end è stata curata da Delia Marino[cite: 40].
- [cite_start]**Backend**: API RESTful basato su Apache Spark per l'elaborazione dei dati[cite: 61].
- [cite_start]**Database**: DBMS MySQL[cite: 61].
- [cite_start]**Obiettivo**: Consente agli utenti di eseguire operazioni CRUD sulle entità del sistema, inviare richieste HTTP all'API Spark e visualizzare i risultati in un'interfaccia reattiva[cite: 62].

## 2. Architettura
[cite_start]L'applicazione è strutturata su un'architettura a livelli[cite: 63]:
- [cite_start]**Presentazione**: Utilizzo di Razor Pages per la gestione dell'interfaccia utente[cite: 63]. [cite_start]I dati sono visualizzati usando modelli fortemente tipizzati[cite: 70].
- [cite_start]**Business Logic**: Utilizzo del Repository pattern per gestire le operazioni CRUD[cite: 64].
- [cite_start]**Dati**: L'elaborazione e gestione dei dati è affidata al servizio Spark (API RESTful)[cite: 65].
- [cite_start]**Comunicazione**: Avviene tramite chiamate HTTP al backend Spark, centralizzate grazie a un Client che sfrutta `HttpClient`[cite: 66, 69]. [cite_start]Gli esiti di operazioni come POST o DELETE sono notificati tramite *toast*[cite: 71]. [cite_start]Tutte le comunicazioni sono sicure (HTTPS crittografato) mediante certificati SSL/TLS[cite: 72].
- [cite_start]**Autenticazione**: Affidata a Keycloak[cite: 66].
- [cite_start]**Portabilità**: Utilizzo di Docker per la configurazione e l'uso di Keycloak[cite: 67].

## 3. Template Dati
I dati scambiati rispecchiano i seguenti modelli:
- [cite_start]**Scuola**: nome, id, città, via, numero civico[cite: 84].
- [cite_start]**Macchinetta**: id, idScuola, cassa, cialde, zucchero, bicchierini e flag vari (eccezioni/problemi)[cite: 85].
- [cite_start]**Richiesta tecnico**: contiene id macchinetta, id scuola e flag che indicano il tipo di problema (cassa piena, esaurimento bicchierini, cialde o zucchero)[cite: 83, 84].

## 4. Route sul Server REST
[cite_start]L'applicazione interagisce con queste route principali esposte dal server[cite: 85]:
- [cite_start]`/help`: Visualizza tutte le route[cite: 85].
- [cite_start]`/elenco/istituti` e `/elenco/macchinette`: Elenco di tutte le scuole e macchinette[cite: 86].
- [cite_start]`/elenco/macchinette/scuola/:idScuola`: Elenco delle macchinette appartenenti a una specifica scuola[cite: 87].
- [cite_start]`/controllo/macchinetta/:idMacchinetta`: Parametri specifici di una macchinetta[cite: 87].
- [cite_start]`/inviaTecnico`: POST per richiedere l'intervento del tecnico[cite: 88].
- [cite_start]`/riceviRichieste`: GET per recuperare la richiesta di assistenza più recente[cite: 89].
- [cite_start]`/add/scuola` e `/add/:idScuola/macchinetta`: POST per i form di aggiunta scuole e macchinette[cite: 90, 91].
- [cite_start]`/delete/scuola/:idScuola`: Elimina una scuola e automaticamente anche tutte le macchinette ad essa associate[cite: 91, 92].
- [cite_start]`/delete/macchinetta/:idMacchinetta`: Elimina la singola macchinetta[cite: 92].

## 5. Autenticazione e Ruoli (Demo Web)
[cite_start]All'avvio, la web app si collega a Keycloak per l'autenticazione[cite: 74]. [cite_start]L'accesso avviene tramite username e password e il token definisce l'indirizzamento al menù corretto[cite: 76]. [cite_start]I ruoli previsti sono tre[cite: 75]:

1. [cite_start]**Amministratore**: Può visualizzare, aggiungere o eliminare scuole e macchinette[cite: 77].
2. [cite_start]**Impiegato**: Può esclusivamente visualizzare l'elenco di scuole e macchinette[cite: 77].
3. [cite_start]**Tecnico**: Visualizza le richieste di guasto e può eliminarle una volta risolte[cite: 77].

**Comportamento Interfaccia**:
- [cite_start]Le macchinette guaste sono segnalate con un simbolo rosso; cliccandolo, i dettagli del guasto appaiono sotto la tabella[cite: 78].
- [cite_start]Cliccando su una specifica scuola si apre una modale con l'elenco delle relative macchinette[cite: 79].
- **Logout**: Per cambiare utente è necessario effettuare il logout per poi riavviare l'applicazione manualmente. [cite_start]In assenza di logout, la sessione viene mantenuta attiva[cite: 80, 81].

## 6. Istruzioni per l'Avvio
### Importazione del Database
1. [cite_start]Creare in MySQL un DB chiamato `ProgettoMacchinetteParteAmministrativa` (porta 3306, utente `root`, password `zLfVSEAz8rKaUi`)[cite: 92].
2. [cite_start]Importare il file di backup `.sql` presente nella cartella `backupmysql`[cite: 93].

### Avvio Web App e Keycloak
1. [cite_start]Avviare il Server REST (es. tramite Eclipse) e lasciarlo in esecuzione[cite: 93].
2. [cite_start]Aprire Docker Desktop e avviare il terminale Windows nella cartella: `...\Progetto reti 2\progetto-reti 2\VisualStudio\AziendaMacchinetteBevande\`[cite: 94].
3. [cite_start]Lanciare il comando `docker-compose up` e lasciarlo caricare in background[cite: 95].
4. [cite_start]Aprire la soluzione `AziendaMacchinetteBevande.sln` su Visual Studio[cite: 96].
5. [cite_start]Eseguire l'app in modalità HTTPS (tasto Play con https)[cite: 97].
6. [cite_start]Effettuare il login nella schermata di Keycloak che si aprirà automaticamente[cite: 98].

### [cite_start]Credenziali di Test [cite: 99]
- **Amministratore**: `amministratore-user` / `ciaoatutti4`
- **Impiegato**: `impiegato-user` / `ciao99:-)`
- **Tecnico**: `tecnico-user` / `chiaveInglese`

### ⚠️ Attenzione sui problemi di Docker/Keycloak
[cite_start]A causa di criticità interne al sistema di backup di Keycloak nell'utilizzo con Docker, il container si avvia tramite un backup parziale[cite: 101, 102]. [cite_start]Questo file precarica la configurazione dei client (la web app) e dei ruoli, ma **non** gli utenti[cite: 101]. 
[cite_start]Di conseguenza, prima dell'esecuzione è necessario creare manualmente gli utenti dalla console amministrativa di Keycloak o operare direttamente sul PC dove l'ambiente è già stato configurato[cite: 103].# Documentazione Web App (Parte Gestionale)
