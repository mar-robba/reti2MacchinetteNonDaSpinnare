package com.smartfeeder.cassa;

import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.common.TopicUtils;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Microservizio CassaAttesa.
 * Thread MQTT che ascolta messaggi dall'InterfacciaUtente e comunica
 * con Erogatore e Assistenza via broker Mosquitto.
 */
public class CassaAttesa implements MqttCallback {

    private final int idDistributore;
    private final String mqttUsername;
    private final String mqttPassword;
    private MqttClient mqttClient;

    private final Cassa cassa;
    private volatile boolean guasto = false;

    public CassaAttesa(int idDistributore, String password) {
        this.idDistributore = idDistributore;
        this.mqttUsername = TopicUtils.CASSA + idDistributore;
        this.mqttPassword = password;
        this.cassa = new Cassa(idDistributore);
    }

    /**
     * Avvia il microservizio: connessione broker e subscribe ai topic.
     */
    public void startListening() {
        try {
            mqttClient = MqttClientFactory.createAndConnect(
                    mqttUsername, "smartfeeder", mqttPassword, this);

            // Ascolta messaggi dall'InterfacciaUtente
            mqttClient.subscribe(TopicUtils.interfacciaUtenteCassa(idDistributore), 1);
            // Ascolta comandi dall'Assistenza (es. cassa svuotata)
            mqttClient.subscribe(TopicUtils.assistenzaCassa(idDistributore), 1);

            System.out.println("[Cassa] Microservizio avviato. ID distributore: " + idDistributore);

            // Keep alive
            while (!guasto && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }
        } catch (MqttException e) {
            System.err.println("[Cassa] Errore MQTT: " + e.getMessage());
            invioGuasto();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cassa.close();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[Cassa] Messaggio su " + topic + ": " + payload);

        // Messaggio dall'InterfacciaUtente: moneta inserita
        if (topic.equals(TopicUtils.interfacciaUtenteCassa(idDistributore))) {
            if (payload.equals(TopicUtils.MSG_INSERISCI_MONETA)) {
                if (cassa.inserimentoMoneta()) {
                    // Comunica all'Erogatore di erogare la porzione
                    publishTo(TopicUtils.MSG_EROGA, TopicUtils.cassaErogatore(idDistributore));
                    // Notifica UI
                    publishTo("Moneta accettata", TopicUtils.cassaInterfaccia(idDistributore));

                    // Controllo cassa piena dopo inserimento
                    if (cassa.cassaPiena()) {
                        publishTo(TopicUtils.MSG_CASSA_PIENA,
                                  TopicUtils.cassaAssistenza(idDistributore));
                    }
                } else {
                    // Cassa piena, rifiuta moneta
                    publishTo(TopicUtils.MSG_CASSA_PIENA,
                              TopicUtils.cassaAssistenza(idDistributore));
                }
            }
        }

        // Comandi dall'Assistenza
        else if (topic.equals(TopicUtils.assistenzaCassa(idDistributore))) {
            if (payload.equals("cassa svuotata") || payload.contains(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA)) {
                cassa.svuotaCassa();
                guasto = false;
                System.out.println("[Cassa] Ricevuto segnale di riparazione/svuotamento.");
            }
        }
    }

    private void publishTo(String messaggio, String topic) {
        try {
            mqttClient.publish(topic, new MqttMessage(messaggio.getBytes()));
        } catch (MqttException e) {
            System.err.println("[Cassa] Errore pubblicazione: " + e.getMessage());
        }
    }

    private void invioGuasto() {
        guasto = true;
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(TopicUtils.cassaAssistenza(idDistributore),
                        new MqttMessage(TopicUtils.MSG_GUASTO_CASSA.getBytes()));
            }
        } catch (MqttException e) {
            System.err.println("[Cassa] Errore invio guasto: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Cassa] Connessione persa: " + cause.getMessage());
        invioGuasto();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* unused */ }
}
