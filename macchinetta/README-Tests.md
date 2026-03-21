# Documentazione JUnit Tests - Modulo Macchinetta

Questo documento spiega come sono stati strutturati i test JUnit per il modulo `macchinetta` e come eseguirli.

## Struttura dei Test

I test sono stati scritti utilizzando **JUnit 5** e sono posizionati nella directory `src/test/java/microservizi/`. Le classi coperte dai test unitari sono le componenti "core" della logica di business:

1. **`DBManagementTest`**: Verifica l'inizializzazione del database e la corretta generazione delle tabelle (bevande, cialde, cassa) tramite il file SQL di setup.
2. **`CassaTest`**: Verifica la logica di inserimento delle monete (accettate e rifiutate), il controllo del credito rispetto al prezzo e il calcolo del resto, oltre al corretto aggiornamento del credito totale nel database locale.
3. **`ErogatoreTest`**: Verifica il controllo della disponibilità delle cialde prima dell'erogazione, l'aggiornamento corretto del numero di cialde/bicchieri/zucchero nel database e il sistema di check delle soglie (es. quando le cialde o lo zucchero stanno per finire).
4. **`InterfacciautenteTest`**: Verifica i metodi interni dell'interfaccia utente (recupero informazioni bevande ed elenchi prezzi) slegati dall'implementazione visiva Swing (che è testata solitamente con framework end-to-end e non è lo scopo degli unit test).

### Isolamento dell'Ambiente di Test
Tutti i test generano un *database SQLite temporaneo e isolato* (es. `macchinetta_999_test.db` o simili) inizializzandolo ad ogni esecuzione e **cancellandolo al termine del test**. 
Questo garantisce che:
- L'esecuzione dei test non sporchi i dati del database in produzione.
- Ogni test-case parta da una situazione "pulita", evitando *side-effects* (effetti collaterali in cui un test precedente influenza il successivo).

## Come eseguire i test

Dal momento che il progetto utilizza **Maven**, l'esecuzione dei test è completamente automatizzata tramite la riga di comando.

### 1. Esecuzione base tramite terminale (Veloce)

Posizionarsi, tramite il terminale o console, all'interno della cartella `macchinetta` (quella che contiene il file `pom.xml`):

```bash
cd percorso/del/progetto/macchinetta
mvn test
```

Questo comando scaricherà eventuali dipendenze (se è la prima esecuzione), compilerà i file del progetto, compilerà i test ed eseguirà la suite JUnit per intero. Alla fine verrà mostrato un piccolo "BUILD SUCCESS" o "BUILD FAILURE" con il resoconto dei test eseguiti.

### 2. Esecuzione pulita

Se hai fatto di recente molte modifiche e vuoi essere sicuro che non ci siano vecchi file `.class` a causare anomalie:

```bash
mvn clean test
```

Questo elimina la directory `target/` ricompilando il progetto da zero, prima di eseguire i test.

## Lettura dei Report

Dopo aver eseguito i test, Maven genera un report verbale sul terminale:
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Inoltre, dei report più dettagliati in formato testo ed XML vengono generati nella cartella:
`target/surefire-reports/`

## Esecuzione dei Test tramite l'IDE 

Se utilizzi un IDE come **IntelliJ IDEA**, **Eclipse** o **Visual Studio Code**, puoi:
1. Aprire la cartella `macchinetta`.
2. Sincronizzare il progetto Maven.
3. Espandere la cartella `src/test/java/microservizi`.
4. Cliccare con il tasto destro su un singolo file (es: `CassaTest.java`) oppure sull'intera cartella `microservizi`.
5. Cliccare su **"Run 'CassaTest'"** o **"Run All Tests"**.
6. I risultati appariranno nella tab sottostante con una comoda interfaccia visiva verde/rossa per identificare subito i test falliti.
