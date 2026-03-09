# Documentazione Lato Server - Progetto PISSIR

## 1. Architettura Generale e Tecnologie
* [cite_start]La parte amministrativa del progetto è strutturata come un'applicazione web sviluppata con il framework ASP.NET (utilizzando RazorPages) server-side. [cite: 60, 61]
* [cite_start]Questa applicazione interagisce con un backend RESTful sviluppato in Apache Spark, che si occupa della gestione e dell'elaborazione dei dati. [cite: 61, 65]
* [cite_start]Il backend si connette a un database relazionale MySQL per il salvataggio persistente dei dati. [cite: 61]
* [cite_start]La comunicazione tra la web app e le API esposte da Spark avviene tramite richieste HTTP, gestite in modo centralizzato da un Client che sfrutta la classe `HttpClient`. [cite: 66, 69]
* [cite_start]Per garantire la sicurezza, l'intero sistema utilizza il protocollo HTTPS con l'ausilio di certificati SSL/TLS, proteggendo la comunicazione client-server tramite crittografia. [cite: 72]
* [cite_start]L'autenticazione lato server è gestita centralmente tramite Keycloak (containerizzato con Docker). [cite: 66, 67]

## 2. Server REST e API (Endpoint)
[cite_start]Il backend server espone diverse route (API REST) per l'elaborazione dei dati e la gestione delle entità (scuole e macchinette): [cite: 85]
* [cite_start]`/help`: visualizza tutte le route disponibili sul server. [cite: 85]
* [cite_start]`/elenco/istituti`: fornisce in risposta l'elenco di tutte le scuole. [cite: 86]
* [cite_start]`/elenco/macchinette`: fornisce in risposta l'elenco completo di tutte le macchinette. [cite: 86]
* [cite_start]`/elenco/macchinette/scuola/:idScuola`: restituisce l'elenco delle macchinette associate a uno specifico istituto. [cite: 87]
* [cite_start]`/controllo/macchinetta/:idMacchinetta`: restituisce l'elenco completo dei parametri di stato di una singola macchinetta. [cite: 87]
* [cite_start]`/inviaTecnico`: endpoint in POST per inviare i dati necessari a richiedere un intervento di assistenza. [cite: 88]
* [cite_start]`/riceviRichieste`: endpoint in GET per recuperare la richiesta di assistenza più recente. [cite: 89]
* [cite_start]`/add/scuola`: endpoint in POST che riceve i dati del form per aggiungere una nuova scuola. [cite: 90]
* [cite_start]`/add/:idScuola/macchinetta`: endpoint in POST per la registrazione di una nuova macchinetta all'interno di una determinata scuola. [cite: 91]
* [cite_start]`/delete/scuola/:idScuola`: elimina una scuola specifica e, a cascata, tutte le macchinette ad essa collegate. [cite: 91, 92]
* [cite_start]`/delete/macchinetta/:idMacchinetta`: elimina una singola macchinetta. [cite: 92]

## 3. Comunicazione Macchinetta-Server (MQTT via Mosquitto)
[cite_start]Le macchinette fisiche non comunicano direttamente in HTTP con il backend, ma passano per il message broker Mosquitto. [cite: 48]
* [cite_start]All'interno del sistema Mosquitto, il `serverREST` è censito come un utente univoco (ne esiste solo uno). [cite: 46]
* [cite_start]I topic di comunicazione rispettano la struttura: `/macchinetteReti2/id_macchinetta/nome_microservizio_mittente/nome_microservizio_destinatario/`. [cite: 48]
* [cite_start]**Dal Server alle Macchinette**: il server comunica principalmente con l'interfaccia utente (passando per il nodo di "assistenza") inviando messaggi come `"riparazione effettuata - interfaccia_utente"` oppure `"problemi nella riparazione - interfaccia_utente"`. [cite: 48]
* **Dalle Macchinette al Server**:
  * [cite_start]Tramite l'Assistenza e la Cassa, il server riceve: `"La cassa è piena"`, `"Si è verificato un guasto - cassa"` e log di sincronizzazione `"sync json cassa-server + json"`. [cite: 48]
  * [cite_start]Dall'interfaccia utente riceve segnali di guasto: `"Si è verificato un guasto - interfaccia_utente"`. [cite: 48]
  * [cite_start]Dall'erogatore arrivano log di guasto specifici e sincronizzazioni di dati: `"Si è verificato un guasto - erogatore"`, `"sync json erogatore-server + json"`. [cite: 48, 49]

## 4. Rilevamento Anomalie e Gestione dei Guasti del Server
* [cite_start]**Rilevamento Guasti Macchinette**: L'unico punto di connessione tra il Server REST e la macchinetta è il microservizio di "Assistenza". [cite: 51] In situazioni normali, l'assistenza manda continui segnali di ping al server. [cite_start]Se questi si interrompono per un determinato lasso di tempo, il server considera la macchinetta come "guasta". [cite: 51, 52] [cite_start]A quel punto, il server scollega il subscriber associato e salva autonomamente nel database una richiesta per guasto generico. [cite: 52]
* **Guasto Critico del ServerREST**: Nel caso in cui il ServerREST stesso dovesse subire un'interruzione, il sistema entra in uno stato critico. [cite_start]I tecnici non potranno più visualizzare le richieste sull'applicazione web. [cite: 56] [cite_start]Una volta riparato il server, il personale è tenuto a ispezionare manualmente tutte le macchinette (o rispondere alle chiamate delle scuole), in quanto le richieste di riparazione generate durante il tempo di inattività del server andranno perse irrimediabilmente. [cite: 57, 59, 60]

## 5. Setup, Avvio e Database Server
* [cite_start]**Configurazione MySQL**: Il backend si aspetta la presenza in locale di un database MySQL in esecuzione sulla porta 3306. [cite: 92] [cite_start]Il db deve essere rinominato in `ProgettoMacchinetteParteAmministrativa`, accessibile con utente `root` e password `zLfVSEAz8rKaUi`. [cite: 92] [cite_start]È possibile importare la struttura dal backup SQL fornito. [cite: 93]
* **Ordine di Avvio**: Il Server REST va lanciato per primo. [cite_start]Deve essere avviato come progetto Maven da Eclipse e lasciato in esecuzione in background. [cite: 93] [cite_start]Solo successivamente vanno avviati i container Docker (per Keycloak) e, infine, l'interfaccia dell'applicazione ASP.NET su Visual Studio (con avvio rigorosamente in modalità HTTPS). [cite: 94, 96, 97]
