package microservizi.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence; // ?

import java.util.UUID; // ?

/**
 * Publisher MQTT per inviare messaggi a topic specifici.
 */
public class MQTTPublisher {

    private MqttClient client;

    /**
     * Crea un publisher MQTT connesso al broker.
     */
    public MQTTPublisher(MQTTConfig config) {
        try {
                               // cosa serve assegnare un id univoco al client, non è una operazione che può essere fatta in automatico
            String clientId = "pub-" + UUID.randomUUID().toString().substring(0, 8);
                                                                            // ?
            client = new MqttClient(config.getBrokerUrl(), clientId, new MemoryPersistence());
           // si creano le opzioni e lassociazione all'utente cui il client e associato
                                             // forse c'è nell'esempio di emacs
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            //Un singolo utente Mosquitto (ovvero uno specifico username e password) può essere utilizzato da più client contemporaneamente.
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                options.setUserName(config.getUsername());
                options.setPassword(config.getPassword().toCharArray());
            }

            client.connect(options); //lo si assoccia e connette con le opzioni e naturalemte anche al'utente della configurazione. Inoltre ha il relativo id client
            System.out.println("[MQTT-PUB] Connesso al broker: " + config.getBrokerUrl());

        } catch (MqttException e) {
            System.err.println("[MQTT-PUB] Errore connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pubblica un messaggio su un topic.
     *
     * @param topic   il topic destinazione
     * @param message il messaggio da inviare
     */
    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1);
                mqttMessage.setRetained(false);
                client.publish(topic, mqttMessage);
                System.out.println("[MQTT-PUB] Inviato su " + topic + ": " + message);
            } else {
                System.err.println("[MQTT-PUB] Client non connesso, impossibile pubblicare.");
            }
        } catch (MqttException e) {
            System.err.println("[MQTT-PUB] Errore pubblicazione: " + e.getMessage());
            e.printStackTrace();
        }
    }
                 // diconneti e chiudi
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            System.err.println("[MQTT-PUB] Errore disconnessione: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
