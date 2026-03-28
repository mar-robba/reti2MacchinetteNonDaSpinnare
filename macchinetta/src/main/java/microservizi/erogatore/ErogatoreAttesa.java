package microservizi.erogatore;

import microservizi.mqtt.MQTTConfig;
import microservizi.mqtt.MQTTPublisher;
import microservizi.mqtt.MQTTSubscriber;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ErogatoreAttesa – Listener MQTT per il microservizio Erogatore.
 * Come da diagramma delle classi pagina 7.
 *
 * Riceve comandi di erogazione dalla Cassa e gestisce le code.
 */
public class ErogatoreAttesa {

    private int idMacchinetta;
    private String mqttUsername;
    private String mqttPassword;
    private String topicRadix;
    private ArrayList<MQTTSubscriber> listaSubscriber;
    private MQTTConfig mqttConfig;

    private BlockingQueue<Integer> numeroCaffeQueue;
    private BlockingQueue<Integer> zuccheroQueue;

    public ErogatoreAttesa(int idMacchinetta, String mqttPassword) {
        this.idMacchinetta = idMacchinetta;
        this.mqttUsername = "macchinetta" + idMacchinetta;
        this.mqttPassword = mqttPassword;
        this.topicRadix = "macchinetta/" + idMacchinetta + "/erogatore/";
        this.listaSubscriber = new ArrayList<>();

        this.numeroCaffeQueue = new LinkedBlockingQueue<>();
        this.zuccheroQueue = new LinkedBlockingQueue<>();
                                        // endpoit dell'host
        this.mqttConfig = new MQTTConfig("localhost", 1883, mqttUsername, mqttPassword);
    }

    /**
     * Routing dei messaggi MQTT per l'erogatore.
     */
    public void sceltaRisposta(String currentTopic, String mex) {
        try {
            // todo: meglio uno switc
                                     // meglio per costanti manifeste
            if (currentTopic.endsWith("/eroga")) {
                int numeroBevanda = Integer.parseInt(mex);
                numeroCaffeQueue.put(numeroBevanda);
            } else if (currentTopic.endsWith("/setZucchero")) {
                int livello = Integer.parseInt(mex);
                zuccheroQueue.put(livello);
            } else {
                System.out.println("[ErogatoreAttesa] Topic non gestito: " + currentTopic);
            }
        } catch (NumberFormatException e) {
            System.err.println("[ErogatoreAttesa] Formato non valido: " + mex);
            // ?? programmazione concorrente che vuol dire mettere lanciare un interrupt al thread corrente come getione dell'eccezione InterruptedExeption
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Pubblica un messaggio MQTT.
     */
    public void publisherTo(String mex, String topic) {
        MQTTPublisher publisher = new MQTTPublisher(mqttConfig);
        publisher.publish(topic, mex);
        publisher.disconnect(); // guarda effetti collaterali del non disconnettere
    }

    /**
     * Avvia tutti i subscriber MQTT per l'erogatore.
     */
    public void startListening() {
        System.out.println("[ErogatoreAttesa] Avvio subscriber su radix: " + topicRadix);
                                                                              // meglio metterli come costanti manifeste
        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "eroga", this::sceltaRisposta));
        listaSubscriber.add(new MQTTSubscriber(mqttConfig, topicRadix + "setZucchero", this::sceltaRisposta));
        System.out.println("[ErogatoreAttesa] " + listaSubscriber.size() + " subscriber attivi.");
    }

    /**
     * Segnala guasto cialde/zucchero/bicchieri all'Assistenza.
     */
    public void invioGuasto(String tipoGuasto) {
        String topicAssistenza = "macchinetta/" + idMacchinetta + "/assistenza/guasto";
        publisherTo(tipoGuasto, topicAssistenza);
        System.out.println("[ErogatoreAttesa] Guasto segnalato: " + tipoGuasto);
    }

    public BlockingQueue<Integer> getNumeroCaffeQueue() { return numeroCaffeQueue; }
    public BlockingQueue<Integer> getZuccheroQueue() { return zuccheroQueue; }
    public ArrayList<MQTTSubscriber> getListaSubscriber() { return listaSubscriber; }
    public String getTopicRadix() { return topicRadix; }

    public void stopListening() {
        for (MQTTSubscriber sub : listaSubscriber) {
            sub.disconnect();
        }
        listaSubscriber.clear();
    }
}
