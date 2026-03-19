# Come Usare il Sistema PISSIR

Guida all'utilizzo del sistema distributore automatico per ciascuno dei 4 ruoli: **Utente Finale**, **Amministratore**, **Impiegato** e **Tecnico**.

---

## 1. Utente Finale (Macchinetta GUI)

L'utente interagisce con il distributore tramite la finestra Swing "Macchinetta".

### Acquistare una bevanda

1. **Visualizzare le bevande**: la lista appare automaticamente all'avvio (es. `1: Caffe`, `2: Latte caldo`, ecc.)
2. **Selezionare la bevanda**: inserire il numero nel campo "Input bevanda" (o usare i tasti numerici) e premere **Inserisci**
3. **Modificare lo zucchero** (opzionale): premere **+** o **-** sulla tastiera (range 0–3)
4. **Inserire monete**: cliccare sui pulsanti moneta (`0.05€`, `0.10€`, `0.20€`, `0.50€`, `1.00€`, `2.00€`). Il credito inserito si aggiorna in tempo reale
5. **Confermare l'acquisto**: premere **Enter**. Se il credito è sufficiente:
   - La bevanda viene erogata (messaggio in "Output bevanda erogata")
   - Il resto viene calcolato e mostrato (in "Monete restituite")
6. Se il credito è insufficiente, viene mostrato un messaggio di errore

### Riavere le monete

- Premere il pulsante rosso **Ridai Monete** in qualsiasi momento
- Le monete inserite vengono restituite e il credito torna a zero

### Annullare la selezione

- Premere **Canc** per resettare tutti i campi e ricominciare

---

## 2. Amministratore (Web App)

Accedere a `http://localhost:5000` → **Amministratore**.

### Aggiungere una scuola

1. Cliccare **Aggiungi Scuola** nel menu
2. Compilare il form (nome obbligatorio, indirizzo/città/provincia/CAP opzionali)
3. Premere **Aggiungi Scuola**
4. Se il nome esiste già, viene mostrato un errore ("Scuola già esistente")
5. Se i dati non sono validi, viene mostrato un errore ("Dati non validi")

### Aggiungere una macchinetta

1. Cliccare **Aggiungi Macchinetta** nel menu
2. Selezionare la scuola dal dropdown (se non esiste, cliccare **Crea Nuova Scuola**)
3. Inserire il nome della macchinetta
4. Premere **Aggiungi Macchinetta**

### Eliminare una scuola

1. Cliccare **Elimina Scuola** nel menu
2. Viene mostrato l'elenco delle scuole
3. Cliccare **Elimina** accanto alla scuola desiderata
4. Confermare nella finestra di dialogo ("Vuoi davvero eliminare questa scuola?")
5. **Attenzione**: vengono eliminate anche tutte le macchinette associate (CASCADE)

### Eliminare una macchinetta

1. Cliccare **Elimina Macchinetta** nel menu
2. Viene mostrato l'elenco di tutte le macchinette con il loro stato
3. Cliccare **Elimina** accanto alla macchinetta desiderata
4. Confermare l'eliminazione

### Visualizzare elenchi

- **Elenco Istituti**: mostra tutte le scuole, cliccando su una si vedono le sue macchinette
- **Elenco Macchinette**: mostra tutte le macchinette con badge di stato (OK, Cassa piena, Cialde, ecc.)
  - Cliccare **Dettaglio** per vedere lo stato completo della macchinetta
  - Se c'è un guasto, appare il pulsante **Invia Tecnico**

---

## 3. Impiegato (Web App)

Accedere a `http://localhost:5000` → **Impiegato**.

### Funzionalità disponibili

- **Elenco Macchinette**: visualizza tutte le macchinette → click su una → dettaglio stato → invia tecnico se guasto
- **Elenco Istituti**: visualizza tutti gli istituti → click su uno → macchinette della scuola

> L'impiegato ha le stesse viste dell'amministratore per elenchi e stati, ma **non** può aggiungere/eliminare scuole o macchinette.

---

## 4. Tecnico (Web App)

Accedere a `http://localhost:5000` → **Tecnico**.

### Gestione richieste di manutenzione

1. La pagina mostra tutte le **richieste aperte** con dettagli:
   - ID, macchinetta, scuola, tipo guasto, stato, data apertura
2. Per ogni richiesta, sono disponibili **azioni contestuali** in base al tipo di guasto:
   - 💰 **Svuota Cassa** → se flag cassa piena
   - ☕ **Riempi Cialde** → se flag cialde in esaurimento
   - 🧂 **Aggiungi Zucchero** → se flag zucchero in esaurimento
   - 🥤 **Riempi Bicchierini** → se flag bicchieri in esaurimento
   - 🔧 **Aggiusta Macchinetta** → se flag guasto generico
3. Dopo aver eseguito l'azione, cliccare **Elimina Richiesta** per chiuderla
4. Se non ci sono richieste aperte, viene mostrato il messaggio "Tutto funziona correttamente"

---

## Scenari d'uso tipici

### Scenario 1: Acquisto caffè con resto

1. Utente seleziona bevanda 1 (Caffè, prezzo 0.50€)
2. Utente inserisce moneta da 1.00€
3. Utente preme Enter
4. Sistema eroga caffè + resto di 0.50€

### Scenario 2: Cassa piena → intervento tecnico

1. La cassa supera la capacità durante un acquisto
2. Il microservizio Cassa segnala `CASSA_PIENA` via MQTT all'Assistenza
3. L'Assistenza inoltra al Server REST
4. Il Server REST crea una richiesta tecnico nel DB MySQL
5. L'impiegato vede il flag sulla macchinetta → clicca **Invia Tecnico**
6. Il tecnico accede alla pagina richieste → clicca **Svuota Cassa** → poi **Elimina Richiesta**

### Scenario 3: Amministratore aggiunge una nuova scuola con macchinetta

1. Amministratore va su **Aggiungi Scuola** → compila il form → conferma
2. Amministratore va su **Aggiungi Macchinetta** → seleziona la nuova scuola → inserisce nome → conferma
3. La macchinetta appare nell'elenco con stato "ATTIVA"
