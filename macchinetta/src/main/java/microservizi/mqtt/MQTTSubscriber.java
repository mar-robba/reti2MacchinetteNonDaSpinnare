package microservizi.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Subscriber MQTT che riceve messaggi su un topic specifico.
 * Utilizza callback per notificare i messaggi ricevuti.
 */
public class MQTTSubscriber {

    private MqttClient client;
    private String topic;
    private BiConsumer<String, String> messageHandler;

    /**
     * Crea un subscriber MQTT.
     *
     * @param config         configurazione del broker
     * @param topic          topic a cui sottoscriversi
     * @param messageHandler callback (topic, messaggio) invocato alla ricezione
     */
    public MQTTSubscriber(MQTTConfig config, String topic, BiConsumer<String, String> messageHandler) {
        this.topic = topic;
        this.messageHandler = messageHandler;

        try {
            String clientId = "sub-" + UUID.randomUUID().toString().substring(0, 8);
            client = new MqttClient(config.getBrokerUrl(), clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                options.setUserName(config.getUsername());
                options.setPassword(config.getPassword().toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("[MQTT-SUB] Connessione persa: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String t, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("[MQTT-SUB] Ricevuto su " + t + ": " + payload);
                    if (messageHandler != null) {
                        messageHandler.accept(t, payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Non usato per subscriber
                }
            });

            client.connect(options);
            client.subscribe(topic, 1);
            System.out.println("[MQTT-SUB] Sottoscritto a: " + topic);

        } catch (MqttException e) {
            System.err.println("[MQTT-SUB] Errore connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getTopic() {
        return topic;
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            System.err.println("[MQTT-SUB] Errore disconnessione: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
