# Appunti Architetturali – Scelte non desumibili dagli schemi progettuali

Questo documento raccoglie le scelte architetturali e implementative fatte durante lo sviluppo del sistema PISSIR che **non sono esplicitamente indicate** nel file `schemiProgettuali.json`.

---

## 1. Struttura Multi-Modulo

**Scelta**: il progetto è stato suddiviso in **3 directory/moduli indipendenti**, ciascuno con il proprio build system:
- `macchinetta/` → Maven (Java)
- `server-rest/` → Maven (Java)
- `webapp/` → dotnet (C#)

**Motivazione**: ciascun componente ha stack tecnologico diverso (Java/MQTT/SQLite vs Java/Spark/MySQL vs C#/RazorPages). La separazione permette build e deploy indipendenti, coerente con l'architettura a microservizi.

---

## 2. Pattern Producer/Consumer con BlockingQueue

**Scelta**: le classi `CassaAttesa` e `ErogatoreAttesa` usano `BlockingQueue<T>` per comunicare con i thread di elaborazione in `CassaMain` e `ErogatoreMain`.

**Motivazione**: i callback MQTT (Eclipse Paho) vengono invocati su thread separati. Le BlockingQueue forniscono sincronizzazione thread-safe senza necessità di lock manuali. Il pattern producer/consumer è la soluzione naturale per disaccoppiare ricezione MQTT ed elaborazione business logic.

**Tipi di code in CassaAttesa**:
- `BlockingQueue<Double> moneteQueue` → monete inserite
- `BlockingQueue<Boolean> ridaiSoldiQueue` → richiesta restituzione
- `BlockingQueue<Integer> numeroCaffeQueue` → numero bevanda selezionata
- `BlockingQueue<Integer> zuccheroQueue` → livello zucchero

---

## 3. Thread Daemon per il ciclo principale

**Scelta**: in `CassaMain` e `ErogatoreMain` i thread che elaborano le code sono impostati come **daemon thread**.

**Motivazione**: i daemon thread vengono automaticamente terminati alla chiusura della JVM, semplificando la gestione del ciclo di vita del microservizio senza dover implementare un meccanismo di shutdown esplicito.

---

## 4. Monete ammesse (hardcoded)

**Scelta**: la lista delle monete ammesse è definita come costante in `Cassa.java`:
```java
List<Double> MONETE_AMMESSE = Arrays.asList(0.05, 0.10, 0.20, 0.50, 1.00, 2.00);
```

**Motivazione**: gli schemi indicano "moneta valida (del tipo ammesso: 5 cent, 10 cent, ...)" (pag.11, nota) ma non specificano l'elenco completo. Si è scelto di ammettere tutte le monete euro standard. I floating point sono gestiti con arrotondamento a 2 decimali per evitare errori di precisione.

---

## 5. Schema Topic MQTT

**Scelta**: i topic MQTT seguono una convenzione gerarchica:
```
macchinetta/{id}/cassa/monete
macchinetta/{id}/cassa/ridaiSoldi
macchinetta/{id}/cassa/numeroBevanda
macchinetta/{id}/erogatore/eroga
macchinetta/{id}/erogatore/setZucchero
macchinetta/{id}/interfaccia/creditoInserito
macchinetta/{id}/interfaccia/errore
macchinetta/{id}/interfaccia/bevandaErogata
macchinetta/{id}/interfaccia/resto
macchinetta/{id}/assistenza/guasto
server/macchinetta/{id}/guasto
server/macchinetta/{id}/guastoRisolto
```

**Motivazione**: gli schemi mostrano i flussi di comunicazione tra microservizi (pagg.5, 14-18) ma non definiscono i nomi dei topic MQTT. La convenzione gerarchica `componente/id/sottocomponente/azione` permette il routing efficiente e l'uso di wildcard per il ServerREST (`server/macchinetta/+/guasto`).

---

## 6. Database SQLite: schema single-table

**Scelta**: ogni macchinetta ha un unico file SQLite con 3 tabelle (`bevande`, `cassa`, `cialde`), dove `cassa` e `cialde` hanno una singola riga (id=1).

**Motivazione**: gli schemi indicano che il DB locale è su "ogni singola macchinetta" (pag.4) e che usa JSON (pag. stakTecnologico). Il metodo `readDBLocalAsJson()` (pag.7) suggerisce una struttura semplice. L'approccio single-row per cassa e cialde semplifica i `SELECT/UPDATE` e riflette lo stato corrente della macchinetta.

---

## 7. Mapping bevande → cialde nel Erogatore

**Scelta**: `aggiornaCialde()` include logica per bevande composite:
- **Mocaccino** (bevanda 4) → decrementa sia `cioccolata` che `caffe`
- **Caffè macchiato** (bevanda 5) → decrementa sia `caffe` che `latte`
- **Cioccolata al latte** (bevanda 7) → decrementa sia `cioccolata` che `latte`

**Motivazione**: gli schemi mostrano il `LinkedHashMap<String, Integer> cialde` (pag.8) ma non specificano le regole di composizione. Le ricette sono state inferite dai nomi delle bevande nel diagramma UI (pag.47).

---

## 8. Soglie di esaurimento (Erogatore)

**Scelta**: soglie hardcoded in `Erogatore.java`:
- Cialde: ≤ 5 unità → segnalazione
- Zucchero: ≤ 10 unità → segnalazione
- Bicchieri: ≤ 5 unità → segnalazione

**Motivazione**: gli schemi menzionano "flag cialde in esaurimento" (pag.29) e "minimo 3 cialde oppure si blocca l'utilizzo dello zucchero" (pag.10) ma non definiscono soglie generali. Le soglie scelte garantiscono un margine sufficiente per l'intervento del tecnico.

---

## 9. Server REST: porta 8081

**Scelta**: Spark Java viene avviato sulla porta 8081 (non la default 4567).

**Motivazione**: la porta 8080 è riservata a Keycloak (docker-compose), quindi il Server REST usa 8081 per evitare conflitti. Questa scelta è coerente con i progetti precedenti (cfr. conversazioni precedenti sulla risoluzione di BindException).

---

## 10. Web App: accesso senza autenticazione in sviluppo

**Scelta**: la Web App permette l'accesso diretto ai menu tramite URL senza richiedere autenticazione in modalità sviluppo.

**Motivazione**: Keycloak richiede Docker in esecuzione e una configurazione realm/client specifica. Per semplificare i test, la navigazione è accessibile direttamente. In produzione, l'autenticazione OpenID Connect è già configurata in `Program.cs` e richiede solo l'avvio del container Keycloak.

---

## 11. ApiService (Web App)

**Scelta**: tutta la comunicazione Web App → Server REST è centralizzata in `Services/ApiService.cs` usando `IHttpClientFactory`.

**Motivazione**: gli schemi di sequenza (pagg.36-44) mostrano "Web App → Server REST → DBMS MySQL" come catena di chiamate. Il pattern Service evita la duplicazione di codice HTTP nelle singole pagine Razor e facilita il testing.

---

## 12. Separazione CassaMain/CassaAttesa e ErogatoreMain/ErogatoreAttesa

**Scelta**: la composizione (non ereditarietà) tra Main e Attesa segue il diagramma delle classi (pag.7) che mostra relazioni di composizione (rombo pieno) tra `CassaMain → Cassa` e `CassaMain → CassaAttesa`.

**Motivazione**: la composizione permette di testare `Cassa` e `CassaAttesa` indipendentemente e di sostituire l'implementazione MQTT con mock nei test.
