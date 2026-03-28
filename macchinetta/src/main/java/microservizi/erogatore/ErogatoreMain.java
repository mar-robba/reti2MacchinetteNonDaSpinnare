package microservizi.erogatore;

import java.util.concurrent.TimeUnit;

/**
 * ErogatoreMain – Entry point del microservizio Erogatore.
 * Come da diagramma delle classi pagina 7.
 */
public class ErogatoreMain {

    private int idMacchinetta;
    private Erogatore erogatore;
    private ErogatoreAttesa erogatoreAttesa;

    public ErogatoreMain(int idMacchinetta, String mqttPassword, String mode) {
        this.idMacchinetta = idMacchinetta;
        this.erogatore = new Erogatore(idMacchinetta, mode);
        this.erogatoreAttesa = new ErogatoreAttesa(idMacchinetta, mqttPassword);
    }

    /**
     * Avvia il microservizio Erogatore.
     */
    public void start() {
        erogatoreAttesa.startListening();
        System.out.println("[ErogatoreMain] Microservizio Erogatore avviato per macchinetta " + idMacchinetta);

        // Thread per gestire livello zucchero
        Thread zuccheroThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Integer livello = erogatoreAttesa.getZuccheroQueue().poll(1, TimeUnit.SECONDS);
                    if (livello != null) {
                        erogatore.setZucchero(livello);
                        System.out.println("[ErogatoreMain] Zucchero impostato a: " + livello);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ErogatoreZuccheroThread");

        // Thread per gestire erogazione bevande
        Thread erogaThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Integer numeroBevanda = erogatoreAttesa.getNumeroCaffeQueue().poll(1, TimeUnit.SECONDS);
                    if (numeroBevanda != null) {
                        erogatore.setNumBevanda(numeroBevanda);

                        if (erogatore.bevandaDisponibile(numeroBevanda)) {
                            // Eroga la bevanda
                            erogatore.aggiornaCialde();
                            System.out.println("[ErogatoreMain] Bevanda " + numeroBevanda + " erogata!");

                            // Notifica l'interfaccia utente
                            String topicBevanda = "macchinetta/" + idMacchinetta + "/interfaccia/bevandaErogata";
                            erogatoreAttesa.publisherTo(String.valueOf(numeroBevanda), topicBevanda);

                            // Controlla scorte e segnala guasti se necessario
                            String statoAlert = erogatore.controlloCialde();
                            if (!"OK".equals(statoAlert)) {
                                erogatoreAttesa.invioGuasto(statoAlert);
                            }
                        } else {
                            // Bevanda non disponibile
                            String topicErrore = "macchinetta/" + idMacchinetta + "/interfaccia/errore";
                            erogatoreAttesa.publisherTo("Bevanda non disponibile (cialde o bicchieri esauriti)", topicErrore);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ErogatoreErogaThread");

        zuccheroThread.setDaemon(true);
        erogaThread.setDaemon(true);
        zuccheroThread.start();
        erogaThread.start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("[ErogatoreMain] Interruzione ricevuta, arresto...");
            erogatoreAttesa.stopListening();
        }
    }

    /**
     * Argomenti: idMacchinetta mqttPassword [devOrTest]
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: ErogatoreMain <idMacchinetta> <mqttPassword> [dev|test]");
            System.exit(1);
        }

        int idMacchinetta = Integer.parseInt(args[0]);
        String mqttPassword = args[1];
        String mode = args.length > 2 ? args[2] : "dev"; // modelità di default
                                                                                    // dev|test
        ErogatoreMain erogatoreMain = new ErogatoreMain(idMacchinetta, mqttPassword, mode);
        erogatoreMain.start();
    }
}
