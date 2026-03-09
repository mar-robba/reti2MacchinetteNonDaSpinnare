# Componente: Assistenza

## Descrizione e Responsabilità
* [cite_start]L'Assistenza ha il compito di segnalare i guasti all'interfaccia utente e al server[cite: 41].
* [cite_start]Costituisce l'unico punto di connessione tra il server della macchinetta e l'esterno[cite: 51]. 
* [cite_start]Invia costantemente un segnale di ping al server; se si verifica un guasto critico o se il server non riceve più il ping per un tot di secondi, l'assistenza viene considerata guasta e viene generata una richiesta d'intervento per il tecnico[cite: 51, 52].

## Interazioni (Input e Output)
* **Input ricevuti da:**
  * [cite_start]*Erogatore:* avvisi per mancanza di cialde, mancanza di bicchieri, mancanza di zucchero o guasto generico[cite: 41].
  * [cite_start]*Interfaccia Utente:* segnalazioni di guasto generico[cite: 41].
  * [cite_start]*Cassa:* segnalazioni di cassa piena o guasto generico[cite: 41].
* **Output inviati a:**
  * [cite_start]*Erogatore:* notifiche di rifornimento (cialde rimesse, bicchierini rimessi, zucchero messo) o guasto aggiustato[cite: 41].
  * [cite_start]*Interfaccia Utente:* segnale di guasto aggiustato[cite: 41].
  * [cite_start]*Cassa:* conferma di cassa svuotata e guasto aggiustato[cite: 41].

## Messaggistica MQTT
* [cite_start]Comunica all'Interfaccia Utente l'abilitazione o disabilitazione della GUI (“abilita gui”, “disabilita gui”) e le problematiche legate alle riparazioni[cite: 48].
