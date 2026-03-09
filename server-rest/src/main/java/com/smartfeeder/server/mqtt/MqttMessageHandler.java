package com.smartfeeder.server.mqtt;

import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.common.TopicUtils;
import com.smartfeeder.server.dao.DistributoreDao;
import com.smartfeeder.server.dao.TicketGuastoDao;
import com.smartfeeder.server.model.TicketGuasto;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Handler MQTT lato server.
 * Sottoscrive ai topic assistenza→serverREST per ricevere
 * segnalazioni di guasto e ping dai distributori.
 */
public class MqttMessageHandler implements MqttCallback {

    private MqttClient mqttClient;
    private final TicketGuastoDao ticketDao = new TicketGuastoDao();
    private final DistributoreDao distributoreDao = new DistributoreDao();

    /**
     * Avvia il listener MQTT in un thread separato.
     */
    public void start() {
        new Thread(() -> {
            try {
                                    // da dove prende l'url del broker ?
                String brokerUrl = MqttClientFactory.getBrokerUrl();
                String username = "smartfeeder";
                String password = "smartfeeder123";

                mqttClient = MqttClientFactory.createAndConnect(
                        "server-rest-mqtt", username, password, this);

                // Sottoscrive a tutti i topic assistenza→server con wildcard
                // Pattern: /smartfeeder/+/assistenza/serverREST/
                String wildcardTopic = TopicUtils.BASE + "/+/" + TopicUtils.ASSISTENZA + "/" + TopicUtils.SERVER_REST + "/";
                mqttClient.subscribe(wildcardTopic, 1);

                System.out.println("[MqttHandler] Sottoscritto a: " + wildcardTopic);

            } catch (MqttException e) {
                System.err.println("[MqttHandler] Errore MQTT: " + e.getMessage());
            }             //metodo del thread che dice a se stesso di incominciare 
        }, "mqtt-handler").start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[MqttHandler] Messaggio da " + topic + ": " + payload);

        // Estrai l'ID distributore dal topic: /smartfeeder/{id}/assistenza/serverREST/
        int idDistributore = extractIdFromTopic(topic);
        if (idDistributore < 0) return;

        // Ping heartbeat
        if (payload.equals(TopicUtils.MSG_PING)) {
            distributoreDao.updateUltimoContatto(idDistributore);
            return;
        }

        // Segnalazioni di guasto → crea ticket
        if (payload.equals(TopicUtils.MSG_CASSA_PIENA)
                || payload.equals(TopicUtils.MSG_MANGIME_ESAURITO)
                || payload.equals(TopicUtils.MSG_GUASTO_CASSA)
                || payload.equals(TopicUtils.MSG_GUASTO_EROGATORE)
                || payload.equals(TopicUtils.MSG_GUASTO_INTERFACCIA)) {

            TicketGuasto ticket = new TicketGuasto();
            ticket.setTipoGuasto(payload);
            ticket.setIdDistributore(idDistributore);
            ticket.setStato("aperta");
            ticketDao.create(ticket);

            distributoreDao.updateStatoGuasta(idDistributore, true);
            System.out.println("[MqttHandler] Ticket creato per distributore "
                    + idDistributore + ": " + payload);
        }
    }

    /**
     * Estrai l'ID distributore dal topic MQTT.
     * Formato: /smartfeeder/{id}/assistenza/serverREST/
     */
    private int extractIdFromTopic(String topic) {
        try {
            String[] parts = topic.split("/");
            // parts: ["", "smartfeeder", "{id}", "assistenza", "serverREST", ""]
            return Integer.parseInt(parts[2]);
        } catch (Exception e) {
            System.err.println("[MqttHandler] Impossibile estrarre ID dal topic: " + topic);
            return -1;
        }
    }

    /**
     * Invia un messaggio MQTT a un distributore (topic serverREST→assistenza).
     */
    public void inviaRiparazione(int idDistributore) {
        try {
            String topic = TopicUtils.serverRestAssistenza(idDistributore);
            mqttClient.publish(topic,
                    new MqttMessage(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA.getBytes()));
            System.out.println("[MqttHandler] Inviata riparazione a distributore " + idDistributore);
        } catch (MqttException e) {
            System.err.println("[MqttHandler] Errore invio riparazione: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MqttHandler] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* unused */ }
}
