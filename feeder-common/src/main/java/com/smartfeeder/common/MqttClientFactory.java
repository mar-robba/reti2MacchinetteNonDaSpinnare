package com.smartfeeder.common;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Factory per creare client MQTT con connessione al broker Mosquitto.
 */
public class MqttClientFactory {

    private static final String DEFAULT_BROKER_URL = "tcp://localhost:1883";

    /**
     * Crea e connette un client MQTT.
     * @param clientId  ID univoco del client (es. "cassa1", "erogatore2")
     * @param username  Username MQTT
     * @param password  Password MQTT
     * @param callback  Callback per messaggi in arrivo
     * @return MqttClient configurato e connesso
     */
    public static MqttClient createAndConnect(String clientId, String username,
                                               String password, MqttCallback callback)
            throws MqttException {
        MqttClient client = new MqttClient(getBrokerUrl(), clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        client.setCallback(callback);
        client.connect(options);
        return client;
    }

    /**
     * Recupera l'URL del broker da variabile d'ambiente o usa il default.
     */
    public static String getBrokerUrl() {
        String envUrl = System.getenv("MQTT_BROKER_URL");
        return (envUrl != null && !envUrl.isEmpty()) ? envUrl : DEFAULT_BROKER_URL;
    }
}
