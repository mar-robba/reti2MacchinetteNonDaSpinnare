package microservizi.cassa;

import java.util.concurrent.TimeUnit;

/**
 * CassaMain – Entry point del microservizio Cassa.
 * Come da diagramma delle classi pagina 7.
 *
 * Compone Cassa + CassaAttesa e avvia il ciclo principale
 * che attende messaggi MQTT e li elabora.
 */
public class CassaMain {

    private int idMacchinetta;
    private Cassa cassa;
    private CassaAttesa cassaAttesa;

    public CassaMain(int idMacchinetta, String mqttPassword, String mode) {
        this.idMacchinetta = idMacchinetta;
        this.cassa = new Cassa(idMacchinetta, mode);
        this.cassaAttesa = new CassaAttesa(idMacchinetta, mqttPassword);
    }

    /**
     * Avvia il ciclo principale del microservizio Cassa.
     * Ascolta le code MQTT e processa le richieste.
     */
    public void start() {
        cassaAttesa.startListening();
        System.out.println("[CassaMain] Microservizio Cassa avviato per macchinetta " + idMacchinetta);

        // Thread per gestire inserimento monete
        Thread moneteThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Double importo = cassaAttesa.getMoneteQueue().poll(1, TimeUnit.SECONDS);
                    if (importo != null) {
                        boolean accettata = cassa.inserimentoMonete(importo);
                        String topicRisposta = "macchinetta/" + idMacchinetta + "/interfaccia/creditoInserito";
                        if (accettata) {
                            cassaAttesa.publisherTo(String.valueOf(cassa.getCassaTemporanea()), topicRisposta);
                        } else {
                            // Moneta non valida: espelli e mostra credito attuale
                            String topicEspelli = "macchinetta/" + idMacchinetta + "/interfaccia/espelliMoneta";
                            cassaAttesa.publisherTo(String.valueOf(importo), topicEspelli);
                            cassaAttesa.publisherTo(String.valueOf(cassa.getCassaTemporanea()), topicRisposta);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CassaMoneteThread");

        // Thread per gestire richiesta restituzione monete
        Thread ridaiSoldiThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Boolean richiesta = cassaAttesa.getRidaiSoldiQueue().poll(1, TimeUnit.SECONDS);
                    if (richiesta != null && richiesta) {
                        double importoRestituito = cassa.ridaiTutto();
                        String topicRisposta = "macchinetta/" + idMacchinetta + "/interfaccia/moneteRestituite";
                        if (importoRestituito > 0) {
                            cassaAttesa.publisherTo(String.valueOf(importoRestituito), topicRisposta);
                        } else {
                            String topicErrore = "macchinetta/" + idMacchinetta + "/interfaccia/errore";
                            cassaAttesa.publisherTo("Non hai inserito monete", topicErrore);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CassaRidaiSoldiThread");

        // Thread per gestire richiesta erogazione (numero bevanda → controllo prezzo → resto)
        Thread erogaThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Integer numeroBevanda = cassaAttesa.getNumeroCaffeQueue().poll(1, TimeUnit.SECONDS);
                    if (numeroBevanda != null) {
                        if (cassa.controlloPrezzo(numeroBevanda)) {
                            // Credito sufficiente: comunica all'erogatore
                            String topicEroga = "macchinetta/" + idMacchinetta + "/erogatore/eroga";
                            cassaAttesa.publisherTo(String.valueOf(numeroBevanda), topicEroga);

                            // Calcola resto
                            double resto = cassa.daiResto(numeroBevanda);
                            String topicResto = "macchinetta/" + idMacchinetta + "/interfaccia/resto";
                            cassaAttesa.publisherTo(String.valueOf(resto), topicResto);

                            // Controlla se la cassa è piena dopo il pagamento
                            if (cassa.cassaPiena()) {
                                cassaAttesa.invioGuasto();
                            }
                        } else {
                            // Credito insufficiente
                            String topicErrore = "macchinetta/" + idMacchinetta + "/interfaccia/errore";
                            cassaAttesa.publisherTo("Credito inserito insufficiente.", topicErrore);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CassaErogaThread");

        moneteThread.setDaemon(true);
        ridaiSoldiThread.setDaemon(true);
        erogaThread.setDaemon(true);

        moneteThread.start();
        ridaiSoldiThread.start();
        erogaThread.start();

        // Mantieni il main thread in vita
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("[CassaMain] Interruzione ricevuta, arresto...");
            cassaAttesa.stopListening();
        }
    }

    /**
     * Entry point del microservizio.
     * Argomenti: idMacchinetta mqttPassword [devOrTest]
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: CassaMain <idMacchinetta> <mqttPassword> [dev|test]");
            System.exit(1);
        }

        int idMacchinetta = Integer.parseInt(args[0]);
        String mqttPassword = args[1];
        String mode = args.length > 2 ? args[2] : "dev";

        CassaMain cassaMain = new CassaMain(idMacchinetta, mqttPassword, mode);
        cassaMain.start();
    }
}
