package com.smartfeeder.erogatore;

/*

le citazioni potrebbero non essere esatte

# Componente: Erogatore

## Descrizione e Responsabilità
* [cite_start]È il componente incaricato di scegliere fisicamente le cialde, i bicchierini e lo zucchero, e di controllare l'effettiva disponibilità delle cialde per la bevanda[cite: 41].

## Interazioni (Input e Output)
* **Input ricevuti da:**
  * [cite_start]*Cassa:* riceve il numero identificativo della bevanda da fare e la quantità di zucchero richiesta[cite: 43].
* **Output inviati a:**
  * [cite_start]*Interfaccia Utente:* invia il segnale di "bevanda erogata"[cite: 43].
  * [cite_start]*Assistenza:* comunica guasti generici e segnala l'esaurimento dei bicchierini, l'esaurimento delle cialde o la mancanza di zucchero[cite: 43].

## Messaggistica MQTT
* [cite_start]Invia all'Assistenza (che reindirizza al Server REST) segnali d'errore come: “Si è verificato un guasto - erogatore” o “Mancano le cialde del tipo: cialda”[cite: 48, 49].
* [cite_start]Invia all'Interfaccia Utente il messaggio di conferma: “bevanda erogata”[cite: 49].
*/

/**
 * Entry point per il microservizio Erogatore.
 * Utilizzo: java -jar feeder-erogatore.jar [idDistributore] [mqttPassword]
 */
public class ErogatoreMain {

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

        System.out.println("[ErogatoreMain] Avvio Erogatore per distributore " + idDistributore);
        ErogatoreAttesa erogatoreAttesa = new ErogatoreAttesa(idDistributore, password);
        erogatoreAttesa.startListening();
    }
}
