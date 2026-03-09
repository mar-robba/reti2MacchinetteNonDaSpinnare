package com.smartfeeder.erogatore;

import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.common.TopicUtils;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Microservizio ErogatoreAttesa.
 * Thread MQTT che ascolta comandi dalla Cassa per erogare mangime.
 * Segnala guasti all'Assistenza se il mangime si esaurisce.
 */
public class ErogatoreAttesa implements MqttCallback {

    private final int idDistributore;
    private final String mqttUsername;
    private final String mqttPassword;
    private MqttClient mqttClient;

    private final Erogatore erogatore;
    private volatile boolean guasto = false;

    public ErogatoreAttesa(int idDistributore, String password) {
        this.idDistributore = idDistributore;
        this.mqttUsername = TopicUtils.EROGATORE + idDistributore;
        this.mqttPassword = password;
        this.erogatore = new Erogatore(idDistributore);
    }

    /**
     * Avvia il microservizio: connessione broker e subscribe ai topic.
     */
    public void startListening() {
        try {
            mqttClient = MqttClientFactory.createAndConnect(
                    mqttUsername, "smartfeeder", mqttPassword, this);

            // Ascolta comandi dalla Cassa
            mqttClient.subscribe(TopicUtils.cassaErogatore(idDistributore), 1);
            // Ascolta comandi dall'Assistenza (es. mangime ricaricato)
            mqttClient.subscribe(TopicUtils.assistenzaErogatore(idDistributore), 1);

            System.out.println("[Erogatore] Microservizio avviato. ID distributore: " + idDistributore);

            // Keep alive
            while (!guasto && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }
        } catch (MqttException e) {
            System.err.println("[Erogatore] Errore MQTT: " + e.getMessage());
            invioGuasto();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            erogatore.close();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[Erogatore] Messaggio su " + topic + ": " + payload);

        // Comando dalla Cassa: eroga porzione
        if (topic.equals(TopicUtils.cassaErogatore(idDistributore))) {
            if (payload.equals(TopicUtils.MSG_EROGA)) {
                if (erogatore.erogaPorzione()) {
                    // Notifica UI che l'erogazione è completata
                    publishTo(TopicUtils.MSG_EROGAZIONE_COMPLETATA,
                              TopicUtils.erogatoreInterfaccia(idDistributore));

                    // Controlla se il mangime si è esaurito dopo l'erogazione
                    if (erogatore.mangimeEsaurito()) {
                        publishTo(TopicUtils.MSG_MANGIME_ESAURITO,
                                  TopicUtils.erogatoreAssistenza(idDistributore));
                    }
                } else {
                    // Mangime già esaurito
                    publishTo(TopicUtils.MSG_MANGIME_ESAURITO,
                              TopicUtils.erogatoreAssistenza(idDistributore));
                }
            }
        }

        // Comandi dall'Assistenza
        else if (topic.equals(TopicUtils.assistenzaErogatore(idDistributore))) {
            if (payload.equals("mangime ricaricato") || payload.contains(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA)) {
                erogatore.ricaricaMangime(100); // ricarica standard
                guasto = false;
                System.out.println("[Erogatore] Ricevuto segnale di ricarica/riparazione.");
            }
        }
    }

    private void publishTo(String messaggio, String topic) {
        try {
            mqttClient.publish(topic, new MqttMessage(messaggio.getBytes()));
        } catch (MqttException e) {
            System.err.println("[Erogatore] Errore pubblicazione: " + e.getMessage());
        }
    }

    private void invioGuasto() {
        guasto = true;
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(TopicUtils.erogatoreAssistenza(idDistributore),
                        new MqttMessage(TopicUtils.MSG_GUASTO_EROGATORE.getBytes()));
            }
        } catch (MqttException e) {
            System.err.println("[Erogatore] Errore invio guasto: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Erogatore] Connessione persa: " + cause.getMessage());
        invioGuasto();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* unused */ }
}
