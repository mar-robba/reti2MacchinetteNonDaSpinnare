# Componente: Erogatore

## Descrizione e Responsabilità
* [cite_start]È il componente incaricato di scegliere fisicamente le cialde, i bicchierini e lo zucchero, e di controllare l'effettiva disponibilità delle cialde per la bevanda[cite: 41].

## Interazioni (Input e Output)
* **Input ricevuti da:**
  * [cite_start]*Cassa:* riceve il numero identificativo della bevanda da fare e la quantità di zucchero richiesta[cite: 43].
* **Output inviati a:**
  * [cite_start]*Interfaccia Utente:* invia il segnale di bevanda erogata[cite: 43].
  * [cite_start]*Assistenza:* comunica guasti generici e segnala l'esaurimento dei bicchierini, l'esaurimento delle cialde o la mancanza di zucchero[cite: 43].

## Messaggistica MQTT
* [cite_start]Invia all'Assistenza (che reindirizza al Server REST) segnali d'errore come: “Si è verificato un guasto - erogatore” o “Mancano le cialde del tipo: cialda”[cite: 48, 49].
* [cite_start]Invia all'Interfaccia Utente il messaggio di conferma: “bevanda erogata”[cite: 49]. 
