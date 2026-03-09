# Documentazione Mosquitto - Progetto Macchinette

## 1. Ruolo nel Sistema e Requisiti
* Mosquitto agisce come Broker MQTT (Message Queue Telemetry Transport) ed è una delle componenti fondamentali (insieme a DBMS e API Server) per le comunicazioni tra i microservizi del sistema.
* Come requisito tecnico, è necessario avere installato Mosquitto in locale oppure su una virtual machine.

## 2. Configurazione e Installazione
* La configurazione di Mosquitto comprende la gestione del file `.pwd`, la creazione degli utenti e la specifica dei permessi di ciascuno all'interno del file `acl_file.txt`.
* La corretta configurazione del broker si trova nella cartella "Configurazione MQTT e TLS".
* Questa cartella include anche una copia dei file `mosquitto.conf`, `mosquitto.pwd` e `acl_file.txt` necessari al corretto funzionamento.

## 3. Sicurezza e Connessione
* La connessione con il broker Mosquitto avviene singolarmente all’avvio di ogni componente.
* Per gestire la sicurezza delle comunicazioni vengono utilizzati tre certificati (con corrispondente chiave): un certificato della CA, un certificato del broker e uno condiviso dai client.

## 4. Utenti Mosquitto
* I tipi di utenti previsti nel sistema sono: `cassa`, `erogatore`, `assistenza`, `interfaccia_utente` e `serverREST` (di quest'ultimo ne esiste solo uno).
* La convenzione per formare lo username di una componente di una specifica macchinetta è l'unione tra il nome della componente e l’id della macchinetta (ad esempio, la cassa della macchinetta 1 si chiamerà `cassa1`).
* Ogni componente può loggarsi con il proprio account (es. `cassa1`, `erogatore1`, `interfaccia_utente1`, `assistenza1`).

## 5. Topic Mosquitto
* La struttura standard utilizzata per la creazione di un topic è la seguente: `/macchinetteReti2/id_macchinetta/nome_microservizio_mittente/nome_microservizio_destinatario/`.

## 6. Messaggi scambiati sui Topic
I messaggi scambiati tra le varie componenti (pubblicati e iscritti sui relativi topic) includono:

* **Da cassa a serverREST (tramite assistenza):** * “La cassa è piena”
  * “Si è verificato un guasto - cassa”
  * “sync json cassa-server + json”

* **Da interfaccia_utente a serverREST (tramite assistenza):**
  * “Si è verificato un guasto - interfaccia_utente”

* **Da serverREST a interfaccia_utente (tramite assistenza):**
  * “riparazione effettuata - interfaccia_utente”
  * “problemi nella riparazione - interfaccia_utente”

* **Tra assistenza e interfaccia_utente:**
  * “abilita gui”
  * “disabilita gui”

* **Da erogatore a serverREST (tramite assistenza):**
  * “Si è verificato un guasto - erogatore”
  * “sync json erogatore-server + json”

* **Tra cassa e interfaccia_utente:**
  * “Questa è la moneta inserita: numero”
  * “Richiesta restituzione soldi”
  * “Bevanda selezionata: numero - numero Zucchero”
  * “Il resto è numero”
  * “Credito non sufficiente”
  * "contenuto cassa temporanea - numero"

* **Tra cassa ed erogatore:**
  * “Eroga la bevanda numero - zucchero”

* **Tra erogatore e interfaccia_utente:**
  * “bevanda erogata"

* **Tra erogatore e assistenza:**
  * “Mancano le cialde del tipo: cialda”

## 7. Testing
* Molti dei test condotti tramite JUnit sono simulazioni dei possibili messaggi che un microservizio potrebbe ricevere dal broker di Mosquitto.
* I test di validazione della comunicazione sono stati eseguiti anche manualmente, utilizzando i comandi publisher e subscriber da terminale per isolare possibili problemi legati al codice Java.
* Il test di connessione al broker da parte del tecnico avviene manualmente durante le fasi di accensione o riparazione della macchinetta.
