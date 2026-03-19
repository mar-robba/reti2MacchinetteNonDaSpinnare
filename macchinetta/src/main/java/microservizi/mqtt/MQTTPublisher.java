package microservizi.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

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
            String clientId = "pub-" + UUID.randomUUID().toString().substring(0, 8);
            client = new MqttClient(config.getBrokerUrl(), clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                options.setUserName(config.getUsername());
                options.setPassword(config.getPassword().toCharArray());
            }

            client.connect(options);
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
