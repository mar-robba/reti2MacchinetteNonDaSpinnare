# Classi del modulo Macchinette

Questo documento descrive in dettaglio tutte le classi presenti all'interno del modulo `macchinette`, il loro scopo principale, come si relazionano tra loro e cosa controllano.

## Package `microservizi.assistenza`

### `Assistenza.java`
- **Scopo Principale**: Agisce come microservizio ricevente per le segnalazioni di guasto della macchinetta (es. cassa piena, cialde esaurite) e si occupa di inoltrarle al Server REST centrale.
- **Relazioni**: Interagisce in ascolto tramite MQTT (`MQTTSubscriber`) con la Cassa, l'Erogatore e l'Interfaccia Utente. Interagisce in pubblicazione (`MQTTPublisher`) col Server centrale.
- **Cosa controlla**: Si iscrive ai topic MQTT legati a guasti e risoluzioni (`assistenza/guasto`, `assistenza/risolto`) e inoltra i dati all'amministrazione globale. Ascolta anche la notifica dell'invio di un tecnico dal server (`tecnicoInviato`).

## Package `microservizi.cassa`

### `Cassa.java`
- **Scopo Principale**: Implementa la logica di dominio e di calcolo della Cassa del distributore automatico.
- **Relazioni**: Viene istanziata e utilizzata principalmente da `CassaMain`. Interagisce col database locale tramite `DBManagement`.
- **Cosa controlla**: Gestisce l'inserimento delle monete, il controllo del credito rispetto al prezzo della bevanda scelta, l'accettazione/rifiuto (in base alle denominazioni di moneta valide), l'aggiornamento e il controllo della capienza totale della cassa sul DB, calcola e restituisce il resto, e permette l'operazione di "svuota cassa" da parte del tecnico.

### `CassaAttesa.java`
- **Scopo Principale**: Funge da ascoltatore (Listener) MQTT dedicato per la Cassa. Mette in comunicazione (thread-safe) i messaggi MQTT ricevuti col ciclo principale della Cassa.
- **Relazioni**: Si relaziona indirettamente con l'Interfaccia Utente (riceve i messaggi inerenti a inserimento monete, richiesta rimborso e conferma bevanda) e segnala all'Assistenza eventuali guasti come la `CASSA_PIENA`.
- **Cosa controlla**: Attraverso code bloccanti (`BlockingQueue`), colleziona i messaggi in ingresso (importi monete, numero bevanda selezionata, livello zucchero richiesto, richiesta resto) instradandoli alla logica della cassa in un contesto concorrente.

### `CassaMain.java`
- **Scopo Principale**: Punto di ingresso (Main) del microservizio indipendente Cassa.
- **Relazioni**: Compone e avvia `Cassa` e `CassaAttesa`. Invia messaggi (Publisher) all'Interfaccia Utente e all'Erogatore.
- **Cosa controlla**: Avvia i thread (demoni) per ascoltare continuamente le code di `CassaAttesa`. Quando i dati arrivano, interroga la `Cassa` (logica) e, a seconda del risultato (credito sufficiente, moneta non valida, etc.), invia eventi MQTT, come il comando di erogazione all'Erogatore (`erogatore/eroga`) o i segnali per l'utente (resto, errori, ecc.).

## Package `microservizi.db`

### `DBManagement.java`
- **Scopo Principale**: Gestisce la connessione e l'interazione con il database SQLite locale del distributore.
- **Relazioni**: È utilizzato in modo composito da `Cassa`, `Erogatore` e `Interfacciautente` (ognuno lo istanzia per query specifiche).
- **Cosa controlla**: Inizializza il database tramite lo script `init_db.sql` se non configurato. Reperisce i dati relativi a bevande, stato della cassa e scorte di cialde/zucchero, restituendoli come strutture JSON per facili serializzazioni, o permettendo l'aggiornamento (es. quando l'erogatore consuma una cialda).

## Package `microservizi.erogatore`

### `Erogatore.java`
- **Scopo Principale**: Logica di dominio del meccanismo di erogazione e controllo magazzino interno della macchinetta.
- **Relazioni**: Viene istanziato e manipolato da `ErogatoreMain` e legge/scrive sul DB locale tramite `DBManagement`.
- **Cosa controlla**: Si interfaccia con lo stoccaggio per verificare se le cialde, lo zucchero o i bicchieri sono sufficienti per una specifica bevanda. Gestisce i decrementi degli ingredienti (selezionando quale cialda usare in base alla bevanda, ad es. latte e caffè per il macchiato). Segnala se le scorte scendono sotto le soglie critiche previste.

### `ErogatoreAttesa.java`
- **Scopo Principale**: Listener MQTT per il componente Erogatore. Riceve i comandi remoti.
- **Relazioni**: Riceve messaggi dalla Cassa (per iniziare l'erogazione) e dall'Interfaccia Utente (per settare il livello di zucchero).
- **Cosa controlla**: Preleva i payload relativi al comando `/eroga` e `/setZucchero` da MQTT, isolandoli in `BlockingQueue` a disposizione di `ErogatoreMain`. Segnala inoltre all'Assistenza i guasti (mancanza ingredienti).

### `ErogatoreMain.java`
- **Scopo Principale**: Main application del microservizio Erogatore.
- **Relazioni**: Fa da ponte tra le logiche in `Erogatore` e i messaggi inseriti da `ErogatoreAttesa`. Pubblica eventi sull'Interfaccia Utente (`/interfaccia/bevandaErogata` o errori).
- **Cosa controlla**: Istanzia i thread per eseguire le erogazioni approvate. Quando un comando `/eroga` arriva dalla Cassa, verifica le disponibilità interrogando `Erogatore`. Se i parametri sono corretti aggiorna i contatori degli ingredienti sul DB e notifica all'utente che il prodotto è erogato.

## Package `microservizi.interfacciautente`

### `Interfacciautente.java`
- **Scopo Principale**: Gestisce la complessa logica e la messaggistica MQTT dell'Interfaccia Utente. Spesso è considerato il vero controller utente.
- **Relazioni**: Comunica ampiamente con Cassa ed Erogatore (tramite MQTT) pubblicando i comandi dell'utente. Dialoga costantemente con la visualizzazione `InterfacciaUtenteGUI`.
- **Cosa controlla**: Instrada le azioni scatenate dalla GUI (inserimento monete, scelta zucchero, bevanda) come messaggi MQTT verso la logica. Gestisce l'ascolto per le risposte (resto, crediti inseriti verificati dalla cassa, erogazione da parte dell'erogatore) aggiornando la vista GUI con callback appropriati. A causa della sua centralità contiene inoltre il `main()` formale di questo nodo.

### `InterfacciaUtenteGUI.java`
- **Scopo Principale**: Gestisce nativamente la finestra e le componenti visive in Swing secondo lo schema dell'applicazione. (View).
- **Relazioni**: Dipende puramente da `Interfacciautente` che gli viene passato a costruzione, agendo da delegato (MVC).
- **Cosa controlla**: Renderizza l'interfaccia (lista bevande, tastierino, output). Acquisisce il raw input dell'utente (click bottoni) richiamando i relativi metodi nella logica per attivare il flusso MQTT. Non prende decisioni sul business, se non semplici vincoli visivi o di range.

## Package `microservizi.mqtt`

### `MQTTConfig.java`
- **Scopo Principale**: DTO (Data Transfer Object) per racchiudere i target e le credenziali del broker Mosquitto.
- **Cosa controlla**: Memorizza URL, username e password.

### `MQTTPublisher.java`
- **Scopo Principale**: Componente riutilizzabile per iniettare l'invio di messaggi verso un broker.
- **Cosa controlla**: Si occcupa di stabilire una piccola connessione volante, di preparare la traccia (`MqttMessage`) e inviarla correttamente alla rete, chiudendo e gestendo gli errori nativi della libreria `Eclipse Paho`.

### `MQTTSubscriber.java`
- **Scopo Principale**: Componente riutilizzabile per ricevere asincronamente i messaggi da un topic prescelto.
- **Cosa controlla**: Installa e lascia in esecuzione il client MQTT impostando i callback. Quando percepisce la stringa in rete per il topic seguito, invoca una callback utente `BiConsumer<String, String>` definita dalla logica ricevente.
