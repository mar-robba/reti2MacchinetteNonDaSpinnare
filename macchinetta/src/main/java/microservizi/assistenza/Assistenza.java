package microservizi.assistenza;

import microservizi.mqtt.MQTTConfig;
import microservizi.mqtt.MQTTPublisher;
import microservizi.mqtt.MQTTSubscriber;

import java.util.ArrayList;

/**
 * Assistenza – Microservizio che riceve segnalazioni di guasto
 * da Cassa, Erogatore e InterfacciaUtente, e le inoltra al ServerREST
 * tramite MQTT.
 * Come da diagramma delle classi pagina 6.
 */
public class Assistenza {

    private int idMacchinetta;
    private String mqttUsername;
    private String mqttPassword;
    private String topicRadix;
    private ArrayList<MQTTSubscriber> listaSubscriber;
    private MQTTConfig mqttConfig;

    public Assistenza(int idMacchinetta, String password) {
        this.idMacchinetta = idMacchinetta;
        this.mqttUsername = "macchinetta" + idMacchinetta;
        this.mqttPassword = password;
        this.topicRadix = "macchinetta/" + idMacchinetta + "/";
        this.listaSubscriber = new ArrayList<>();
        this.mqttConfig = new MQTTConfig("localhost", 1883, mqttUsername, mqttPassword);
    }

    public ArrayList<MQTTSubscriber> getListaSubscribers() { return listaSubscriber; }
    public String getTopicRadix() { return topicRadix; }

    /**
     * Crea un subscriber MQTT.
     */
    private MQTTSubscriber createSubscriber(String topic) {
        MQTTSubscriber sub = new MQTTSubscriber(mqttConfig, topic, this::sceltaRisposta);
        listaSubscriber.add(sub);
        return sub;
    }

    /**
     * Pubblica un messaggio MQTT.
     */
    private void createPublisher(String message, String topic) {
        MQTTPublisher publisher = new MQTTPublisher(mqttConfig);
        publisher.publish(topic, message);
        publisher.disconnect();
    }

    /**
     * Routing dei messaggi ricevuti.
     * Riceve guasti dalla cassa, dall'erogatore, dall'interfaccia utente
     * e li inoltra al server REST.
     */
    protected void sceltaRisposta(String currentTopic, String mex) {
        System.out.println("[Assistenza] Segnalazione ricevuta su " + currentTopic + ": " + mex);

        if (currentTopic.endsWith("/guasto")) {
            // Inoltra la segnalazione al ServerREST via MQTT
            String topicServer = "server/macchinetta/" + idMacchinetta + "/guasto";
            createPublisher(mex, topicServer);
            System.out.println("[Assistenza] Guasto inoltrato al server: " + mex);
        } else if (currentTopic.endsWith("/risolto")) {
            // Il tecnico ha risolto il guasto
            String topicServer = "server/macchinetta/" + idMacchinetta + "/guastoRisolto";
            createPublisher(mex, topicServer);
            System.out.println("[Assistenza] Guasto risolto segnalato al server: " + mex);
        } else {
            System.out.println("[Assistenza] Topic non gestito: " + currentTopic);
        }
    }

    /**
     * Avvia i subscriber per ricevere segnalazioni.
     */
    public void startListening() {
        createSubscriber(topicRadix + "assistenza/guasto");
        createSubscriber(topicRadix + "assistenza/risolto");

        // Subscriber per comandi dal server (es. tecnico inviato)
        createSubscriber("server/macchinetta/" + idMacchinetta + "/tecnicoInviato");

        System.out.println("[Assistenza] " + listaSubscriber.size() + " subscriber attivi per macchinetta " + idMacchinetta);
    }

    public void stopListening() {
        for (MQTTSubscriber sub : listaSubscriber) sub.disconnect();
        listaSubscriber.clear();
    }

    /**
     * Entry point del microservizio Assistenza.
     * Argomenti: idMacchinetta mqttPassword
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: Assistenza <idMacchinetta> <mqttPassword>");
            System.exit(1);
        }

        int idMacchinetta = Integer.parseInt(args[0]);
        String mqttPassword = args[1];

        Assistenza assistenza = new Assistenza(idMacchinetta, mqttPassword);
        assistenza.startListening();

        System.out.println("[Assistenza] Microservizio avviato per macchinetta " + idMacchinetta);

        // Mantieni in vita
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("[Assistenza] Interruzione ricevuta, arresto...");
            assistenza.stopListening();
        }
    }
}
