package com.smartfeeder.ui;

/*
le citazioni potrebbero non essere esatte
# Componente: Interfaccia Utente

## Descrizione e Responsabilità
* [cite_start]Garantisce il corretto utilizzo della macchinetta da parte dell'utente comunicando l'inserimento di monete, la selezione delle bevande e lo stato della macchina (inclusi eventuali guasti)[cite: 26, 41].
* [cite_start]Deve contenere una tastiera, uno schermo e l'output per la bevanda[cite: 26, 27].
* [cite_start]Durante un guasto improvviso, la GUI (Graphical User Interface) si disabilita automaticamente, riavviandosi solo quando il tecnico risolve il problema[cite: 126]. 
* [cite_start]Se lo zucchero è in esaurimento, l'interfaccia non va in blocco critico, ma semplicemente disabilita e smette di erogare l'opzione dello zucchero[cite: 127].

## Interazioni (Input e Output)
* **Input ricevuti da:**
  * [cite_start]*Tastiera/Utente:* inserimento di soldi, selezione dei numeri bevande, quantità di zucchero desiderata e richiesta di restituzione dei soldi[cite: 42].
  * [cite_start]*Erogatore:* segnali di erogazione in corso, bevanda pronta/consegnata[cite: 42].
  * [cite_start]*Assistenza:* conferma che i problemi sono stati risolti[cite: 42].
* **Output inviati a:**
  * [cite_start]*Schermo:* mostra lo stato dell'erogazione, le monete restituite, il prezzo della bevanda selezionata e il contenuto della cassa temporanea[cite: 42].
  * [cite_start]*Cassa:* inoltra i numeri della bevanda e lo zucchero, i dettagli sulle monete appena inserite e le richieste di restituire la cassa temporanea[cite: 42].
  * [cite_start]*Assistenza:* comunica il presentarsi di un guasto generico lato UI[cite: 42].

## Elementi della GUI
* [cite_start]**Pulsanti di controllo Cassa:** "Inserisci" (per aggiungere monete), "Restituzione monete" (svuota la cassa temporanea) e "Monete restituite" (per prelevare il resto fisico)[cite: 116, 117, 118].
* **Tastierino Numerico:**
  * [cite_start]**Numeri da 0 a 9:** selezionano le bevande (mostrate nello schermo)[cite: 119].
  * [cite_start]**Canc:** cancella l'ultima cifra inserita[cite: 120].
  * [cite_start]**+ e -:** regolano la dose di zucchero desiderata[cite: 120].
  * [cite_start]**Enter:** preme per confermare i dati alla cassa per avviare l'erogazione[cite: 121].
* [cite_start]**Bevanda Erogata:** Tasto grande per prelevare materialmente la bevanda una volta che l'erogazione è terminata[cite: 124].
* [cite_start]**Messaggistica MQTT:** Segnala avvisi come “Si è verificato un guasto - interfaccia_utente” all'Assistenza[cite: 48].

*/
/**
 * Entry point per il microservizio InterfacciaUtente.
 * Utilizzo: java -jar feeder-ui.jar [idDistributore] [mqttPassword]
 */
public class InterfacciaUtenteMain {

    public static void main(String[] args) {
        int idDistributore = 1;
        String password = "smartfeeder123";

        if (args.length > 0) {
            try {
                idDistributore = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("ID non valido, uso default: 1");
            }
        }
        if (args.length > 1) {
            password = args[1];
        }

        System.out.println("[InterfacciaUtenteMain] Avvio UI per distributore " + idDistributore);
        InterfacciaUtente ui = new InterfacciaUtente(idDistributore, password);
        ui.avvia();
    }
}
