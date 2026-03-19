package server;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * ServerREST_MQTT – Ponte tra MQTT e il ServerREST.
 * Come da diagramma delle classi pagina 6.
 *
 * Riceve messaggi di stato/guasto dalle macchinette via MQTT
 * e aggiorna il database MySQL di conseguenza.
 */
public class ServerREST_MQTT {

    private MqttClient client;
    private DatabaseManager dbManager;

    public ServerREST_MQTT(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Avvia il client MQTT e si sottoscrive ai topic delle macchinette.
     */
    public void start(String brokerUrl, String username, String password) {
        try {
            client = new MqttClient(brokerUrl, "server-rest-mqtt", new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("[ServerREST_MQTT] Connessione persa: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    processMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.connect(options);

            // Sottoscrivi a tutti i topic di guasto dalle macchinette
            client.subscribe("server/macchinetta/+/guasto", 1);
            client.subscribe("server/macchinetta/+/guastoRisolto", 1);
            client.subscribe("server/macchinetta/+/stato", 1);

            System.out.println("[ServerREST_MQTT] Connesso al broker: " + brokerUrl);
            System.out.println("[ServerREST_MQTT] Sottoscritto a topic macchinette.");

        } catch (MqttException e) {
            System.err.println("[ServerREST_MQTT] Errore avvio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa i messaggi MQTT ricevuti dalle macchinette.
     * Aggiorna i flag nel database e crea richieste per il tecnico.
     */
    private void processMessage(String topic, String payload) {
        System.out.println("[ServerREST_MQTT] Messaggio: " + topic + " = " + payload);

        // Estrai l'ID della macchinetta dal topic: server/macchinetta/{id}/guasto
        String[] parts = topic.split("/");
        if (parts.length < 4) return;

        int idMacchinetta;
        try {
            idMacchinetta = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.err.println("[ServerREST_MQTT] ID macchinetta non valido: " + parts[2]);
            return;
        }

        if (topic.endsWith("/guasto")) {
            handleGuasto(idMacchinetta, payload);
        } else if (topic.endsWith("/guastoRisolto")) {
            handleGuastoRisolto(idMacchinetta, payload);
        } else if (topic.endsWith("/stato")) {
            System.out.println("[ServerREST_MQTT] Stato ricevuto da macchinetta " + idMacchinetta);
        }
    }

    /**
     * Gestisce una segnalazione di guasto.
     */
    private void handleGuasto(int idMacchinetta, String tipoGuasto) {
        System.out.println("[ServerREST_MQTT] Guasto da macchinetta " + idMacchinetta + ": " + tipoGuasto);

        // Aggiorna i flag appropriati nel DB
        if (tipoGuasto.contains("CASSA_PIENA")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_cassa_piena", true);
        }
        if (tipoGuasto.contains("CIALDE") && tipoGuasto.contains("ESAURIMENTO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_cialde_esaurimento", true);
        }
        if (tipoGuasto.contains("ZUCCHERO_ESAURIMENTO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_zucchero_esaurimento", true);
        }
        if (tipoGuasto.contains("BICCHIERI_ESAURIMENTO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_bicchieri_esaurimento", true);
        }
        if (tipoGuasto.contains("GUASTO_GENERICO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_guasto_generico", true);
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "stato", false); // Imposta stato GUASTO
        }

        // Crea una richiesta per il tecnico
        dbManager.creaRichiestaTecnico(idMacchinetta, tipoGuasto, "Segnalazione automatica MQTT");
    }

    /**
     * Gestisce la risoluzione di un guasto.
     */
    private void handleGuastoRisolto(int idMacchinetta, String tipoGuasto) {
        System.out.println("[ServerREST_MQTT] Guasto risolto per macchinetta " + idMacchinetta + ": " + tipoGuasto);

        if (tipoGuasto.contains("CASSA_PIENA")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_cassa_piena", false);
        }
        if (tipoGuasto.contains("CIALDE")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_cialde_esaurimento", false);
        }
        if (tipoGuasto.contains("ZUCCHERO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_zucchero_esaurimento", false);
        }
        if (tipoGuasto.contains("BICCHIERI")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_bicchieri_esaurimento", false);
        }
        if (tipoGuasto.contains("GUASTO_GENERICO")) {
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "flag_guasto_generico", false);
        }
    }

    /**
     * Pubblica un messaggio MQTT verso una macchinetta.
     */
    public void publishToMacchinetta(int idMacchinetta, String subtopic, String message) {
        try {
            if (client != null && client.isConnected()) {
                String topic = "macchinetta/" + idMacchinetta + "/" + subtopic;
                MqttMessage msg = new MqttMessage(message.getBytes());
                msg.setQos(1);
                client.publish(topic, msg);
                System.out.println("[ServerREST_MQTT] Pubblicato: " + topic + " = " + message);
            }
        } catch (MqttException e) {
            System.err.println("[ServerREST_MQTT] Errore pubblicazione: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            System.err.println("[ServerREST_MQTT] Errore stop: " + e.getMessage());
        }
    }
}
