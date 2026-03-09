package com.smartfeeder.cassa;

/*
le citazioni potrebbero non essere esatte
# Componente: Cassa

## Descrizione e Responsabilità
* [cite_start]La cassa si occupa di gestire le monete, tenere traccia del totale dei soldi inseriti, controllare l'importo e gestire il resto[cite: 41].
* [cite_start]Mantiene il denaro inserito dall'utente in una "cassa temporanea" fino all'erogazione effettiva della bevanda, per poter restituire i soldi se l'utente li richiede indietro[cite: 4].
* [cite_start]Possiede una "cassa definitiva" in cui finiscono tutti gli incassi degli acquisti; questa deve essere svuotata dal tecnico quando si riempie[cite: 7].
* [cite_start]Se la cassa è piena, la macchinetta diventa inutilizzabile e viene chiamato automaticamente il tecnico[cite: 8].

## Interazioni (Input e Output)
* [cite_start]**Input ricevuti da:** * *Interfaccia Utente:* (solo se la cassa non è piena) riceve il numero della bevanda, le monete appena inserite, la quantità di zucchero (da passare poi all'erogatore) e l'indicazione se deve restituire le monete[cite: 42, 43].
* **Output inviati a:**
  * [cite_start]*Assistenza:* segnala quando la cassa è piena o se si verifica un guasto generico[cite: 43].
  * [cite_start]*Erogatore:* invia il numero della bevanda da preparare e lo zucchero da aggiungere[cite: 43].

## Messaggistica MQTT
* [cite_start]Invia messaggi all'Assistenza (e al serverREST): “La cassa è piena”, “Si è verificato un guasto - cassa”, e i log per sincronizzare il JSON[cite: 48].
* [cite_start]Comunica con l'Interfaccia Utente inviando: “Questa è la moneta inserita: numero”, “Richiesta restituzione soldi”, “Bevanda selezionata...”, “Il resto è numero”, “Credito non sufficiente”, e "contenuto cassa temporanea"[cite: 49].
* [cite_start]Invia all'Erogatore il comando d'azione: “Eroga la bevanda numero - zucchero”[cite: 49].
*/

/**
 * Entry point per il microservizio Cassa.
 * Utilizzo: java -jar feeder-cassa.jar [idDistributore] [mqttPassword]
 */
public class CassaMain {

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

        System.out.println("[CassaMain] Avvio Cassa per distributore " + idDistributore);
        CassaAttesa cassaAttesa = new CassaAttesa(idDistributore, password);
        cassaAttesa.startListening();
    }
}
