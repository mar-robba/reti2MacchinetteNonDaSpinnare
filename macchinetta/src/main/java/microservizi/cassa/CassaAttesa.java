package microservizi.cassa;

import microservizi.mqtt.MQTTConfig;
import microservizi.mqtt.MQTTPublisher;
import microservizi.mqtt.MQTTSubscriber;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * CassaAttesa – Listener MQTT per il microservizio Cassa.
 * Come da diagramma delle classi pagina 7.
 *
 * Riceve messaggi dall'InterfacciaUtente e li inoltra alla Cassa
 * attraverso BlockingQueue per sincronizzazione thread-safe.
 */
public class CassaAttesa {

    private int idMacchinetta;
    private String mqttUsername;
    private String mqttPassword;
    private String topicRadix;
    private ArrayList<MQTTSubscriber> listaSubscriber;
    private MQTTConfig mqttConfig;

    // Code per comunicazione thread-safe con il ciclo principale
    private BlockingQueue<Double> moneteQueue;
    private BlockingQueue<Boolean> ridaiSoldiQueue;
    private BlockingQueue<Integer> numeroCaffeQueue;
    private BlockingQueue<Integer> zuccheroQueue;

    /**
     * @param idMacchinetta id della macchinetta
     * @param mqttPassword  password MQTT
     */
    public CassaAttesa(int idMacchinetta, String mqttPassword) {
        this.idMacchinetta = idMacchinetta;
        this.mqttUsername = "macchinetta" + idMacchinetta;
        this.mqttPassword = mqttPassword;
        this.topicRadix = "macchinetta/" + idMacchinetta + "/cassa/";
        this.listaSubscriber = new ArrayList<>();

        this.moneteQueue = new LinkedBlockingQueue<>();
        this.ridaiSoldiQueue = new LinkedBlockingQueue<>();
        this.numeroCaffeQueue = new LinkedBlockingQueue<>();
        this.zuccheroQueue = new LinkedBlockingQueue<>();

        this.mqttConfig = new MQTTConfig("localhost", 1883, mqttUsername, mqttPassword);
    }

    /**
     * Routing dei messaggi MQTT in base al topic.
     * Smista i messaggi nelle rispettive BlockingQueue.
     */
    public void sceltaRisposta(String currentTopic, String mex) {
        try {
            if (currentTopic.endsWith("/monete")) {
                double importo = Double.parseDouble(mex);
                moneteQueue.put(importo);

            } else if (currentTopic.endsWith("/ridaiSoldi")) {
                ridaiSoldiQueue.put(Boolean.parseBoolean(mex));

            } else if (currentTopic.endsWith("/numeroBevanda")) {
                int numero = Integer.parseInt(mex);
                numeroCaffeQueue.put(numero);

            } else if (currentTopic.endsWith("/zucchero")) {
                int livello = Integer.parseInt(mex);
                zuccheroQueue.put(livello);

            } else {
                System.out.println("[CassaAttesa] Topic non gestito: " + currentTopic);
            }
        } catch (NumberFormatException e) {
            System.err.println("[CassaAttesa] Formato messaggio non valido: " + mex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[CassaAttesa] Interruzione: " + e.getMessage());
        }
    }

    /**
     * Pubblica un messaggio MQTT.
     *
     * @param mex   il messaggio
     * @param topic il topic (relativo al radix)
     */
    public void publisherTo(String mex, String topic) {
        MQTTPublisher publisher = new MQTTPublisher(mqttConfig);
        publisher.publish(topic, mex);
        publisher.disconnect();
    }

    /**
     * Avvia tutti i subscriber MQTT.
     */
    public void startListening() {
        System.out.println("[CassaAttesa] Avvio subscriber su radix: " + topicRadix);

        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "monete", this::sceltaRisposta));
        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "ridaiSoldi", this::sceltaRisposta));
        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "numeroBevanda", this::sceltaRisposta));
        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "zucchero", this::sceltaRisposta));

        System.out.println("[CassaAttesa] " + listaSubscriber.size() + " subscriber attivi.");
    }

    /**
     * Segnala guasto cassa piena all'Assistenza.
     */
    public void invioGuasto() {
        String topicAssistenza = "macchinetta/" + idMacchinetta + "/assistenza/guasto";
        publisherTo("CASSA_PIENA", topicAssistenza);
        System.out.println("[CassaAttesa] Guasto segnalato: CASSA_PIENA");
    }

    // Getter per le code (usati dal ciclo principale in CassaMain)
    public BlockingQueue<Double> getMoneteQueue() { return moneteQueue; }
    public BlockingQueue<Boolean> getRidaiSoldiQueue() { return ridaiSoldiQueue; }
    public BlockingQueue<Integer> getNumeroCaffeQueue() { return numeroCaffeQueue; }
    public BlockingQueue<Integer> getZuccheroQueue() { return zuccheroQueue; }

    public ArrayList<MQTTSubscriber> getListaSubscriber() { return listaSubscriber; }
    public String getTopicRadix() { return topicRadix; }

    /**
     * Disconnette tutti i subscriber.
     */
    public void stopListening() {
        for (MQTTSubscriber sub : listaSubscriber) {
            sub.disconnect();
        }
        listaSubscriber.clear();
        System.out.println("[CassaAttesa] Tutti i subscriber disconnessi.");
    }
}
