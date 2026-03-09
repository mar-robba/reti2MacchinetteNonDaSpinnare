
# importante!
Questo readme è stato scritto dall'ai per velocizzare la situa , tranne questa sezione che l'ho scritta io : Marco.

Ulteriore documentazione la trovare Dentro z_doc dove è presente, per alcuni componenti, sia la documtazione di come attualmente è implementato l'esempio dimostrativo(generata dall'ai), sia gli estratti dal file 'Documentazione - Template di riferimento.pdf'. 

L'applicazione d'esempio (così vi fate un'idea) è quella  di un distibutore di mangime: se si schiaccia un mucchio di volte il pulsante di "inserisci moneta"  la cassa si riempe(compare un allert che tipo la cassa è fuori servizio) e deve essere svuotata dall'interfaccia web di amministrazione, dopodiche tornerà a funzionare. Tutto qua; fatto per testare e esemplificare la situa. La documentazione ufficiale data dal prof è Documentazione - Template di riferimento.pdf presente in /z_doc tutto il resto è roba che ho fatto io e l'ai. Ho messo anche dei commenti in giro, alcuni presentato proprio degli estratti di documentazioneUfficiale(Documentazione - Template di riferimento.pdf') che vuol dire che in quel file, o nel gruppo di file "vicini", dove è stato aggiunto il commento, si dovrà implementare proprio quelle cose indicate nel commento, nulla di più nulla di meno. Naturalmente i diagrammi sono presenti nella doc ufficiale. Approposito di diagrammi bisogna implementare esattamente come è indicato nei diagrammi, quindi dite alle ai di sequire quelli. Cio che non c'è nella doc ufficiale è dato da risolvere al nostro igegno o quello dell'ai.    
Fate quello che vi pare spaccate anche tutto che tanto abbiamo tutte le versioni precedenti grazie a git-hub
### le porta per l'applicazione web è la 5000: 
### l'user e la password per il login sono rispettivamente admin admin.

## Strumenti ai
Per programmare con l'ai scaricate Antigravity e fate un mucchi di gmail per consumare periodi di prova gratuiti. (io fino ad ora ne ho usate 3, secondo me è fattibile per ogni email ci sono molti usi). 
I modelli più potenti al momento della scrittura di questo file sono: claude opus 4.6 thinking e gemini pro 3.1 high.
## todo 
- [ ] non va bene che il generaDb lo faccia con i file .db di sqlite, il prof vuole che tali db locali siano implementati attraverso file json
# AI :
# 🐦 Smart Feeder — Project Skeleton


Benvenuto nel progetto **Smart Feeder**. Questa repository rappresenta uno scheletro architettonico semplificato per un sistema distribuito di gestione macchinette (vending machines), realizzato come esempio per il corso di **Reti 2**.

Il progetto implementa un'architettura completa che spazia dall'infrastruttura Docker ai microservizi Edge in Java, passando per un backend REST e una Web App gestionale in .NET.
---

## 🏗️ Architettura del Sistema

Il sistema è composto dai seguenti blocchi logici:

1.  **Infrastruttura (Docker)**: Include un database MySQL per la persistenza centrale, un broker MQTT (Mosquitto) per la comunicazione tra macchinette e server, e Keycloak per l'Identity and Access Management.
2.  **Server REST (Java/SparkJava)**: Il cuore del backend che espone le API per la gestione dei distributori e comunica con il database centrale.
3.  **Distributore Edge (Microservizi Java)**: Ogni macchinetta è composta da 4 microservizi che collaborano tramite MQTT:
    - `Cassa`: Gestisce i pagamenti e il credito.
    - `Erogatore`: Gestisce l'erogazione fisica dei prodotti.
    - `Assistenza`: Monitora lo stato e invia allarmi.
    - `Interfaccia Utente (UI)`: Una GUI (Swing) per l'interazione con l'utente.
4.  **Web App (ASP.NET Core)**: Un'interfaccia web per monitorare lo stato dei distributori in tempo reale.

---

## 🛠️ Prerequisiti

Prima di iniziare, assicurati di avere installato:
- **Docker** & **Docker Compose**
- **Java 21+** & **Maven**
- **.NET 8 SDK**
- **Bash shell** (per eseguire gli script `.sh`)

---

## 🚀 Come avviare l'esempio

Il progetto è predisposto con una serie di script numerati per semplificare l'avvio in sequenza. Segui questi passi dalla root della repository:

### 1. Avvia l'infrastruttura
Prepara i container Docker (MySQL, Mosquitto, Keycloak).
```bash
./01-start-infrastructure.sh
```
*Attendi che lo script confermi che i servizi sono pronti.*

### 2. Build del codice Java
Compila tutti i moduli back-end e i microservizi.
```bash
./02-build-java.sh
```

### 3. Crea i database locali
Ogni distributore edge necessita di un proprio database SQLite locale.
```bash
./03-create-local-dbs.sh
```

### 4. Avvia il Server REST
Avvia il server backend che sarà in ascolto sulla porta predefinita (8081).
```bash
./04-start-server-rest.sh
```

### 5. Avvia un Distributore (Esempio)
Puoi avviare la simulazione di un distributore specifico (es. ID 1). Verranno aperti i 4 microservizi associati e la GUI.
```bash
./05-start-distributore.sh 1
```

### 6. Avvia la Web App
Infine, avvia la dashboard web per visualizzare lo stato dei distributori.
```bash
./06-start-webapp.sh
```
La Web App sarà accessibile su `http://localhost:5000`.

---

## 🛑 Arresto del Sistema

Per terminare tutti i processi (Java, .NET e Docker) in sicurezza, utilizza lo script di pulizia:
```bash
./07-stop-all.sh
```

---

## 📂 Struttura delle Cartelle

- `server-rest`: Codice sorgente del backend API.
- `feeder-*`: Moduli dei microservizi del distributore (Edge).
- `webapp`: Progetto ASP.NET Core Razor Pages.
- `docker-compose.yml`: Configurazione dei servizi infrastrutturali.
- `z_doc`: Documentazione tecnica aggiuntiva.
