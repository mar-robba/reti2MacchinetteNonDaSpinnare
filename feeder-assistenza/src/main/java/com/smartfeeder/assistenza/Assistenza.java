package com.smartfeeder.assistenza;

import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.common.TopicUtils;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Microservizio Assistenza.
 * "Ponte" diagnostico tra il distributore e il ServerREST.
 * Ascolta i problemi provenienti dalla Cassa e dall'Erogatore e,
 * in caso di anomalia critica, invia una richiesta d'intervento
 * al Server Centrale, disabilitando l'Interfaccia Utente.
 */
public class Assistenza implements MqttCallback {

    private final int idDistributore;
    private final String mqttUsername;
    private final String mqttPassword;
    private MqttClient mqttClient;

    private Timer pingTimer;
    private volatile boolean guasto = false;

    public Assistenza(int idDistributore, String password) {
        this.idDistributore = idDistributore;
        this.mqttUsername = TopicUtils.ASSISTENZA + idDistributore;
        this.mqttPassword = password;
    }

    /**
     * Avvia il microservizio: subscribe a tutti i topic interni, init ping.
     */
    public void startListening() {
        try {
            mqttClient = MqttClientFactory.createAndConnect(
                    mqttUsername, "smartfeeder", mqttPassword, this);

            // Ascolta messaggi da Cassa
            mqttClient.subscribe(TopicUtils.cassaAssistenza(idDistributore), 1);
            // Ascolta messaggi da Erogatore
            mqttClient.subscribe(TopicUtils.erogatoreAssistenza(idDistributore), 1);
            // Ascolta messaggi da InterfacciaUtente
            mqttClient.subscribe(TopicUtils.interfacciaUtenteAssistenza(idDistributore), 1);
            // Ascolta risposte dal ServerREST
            mqttClient.subscribe(TopicUtils.serverRestAssistenza(idDistributore), 1);

            System.out.println("[Assistenza] Avviata. ID distributore: " + idDistributore);

            // Avvia ping periodico al server (ogni 30 secondi)
            startPing();

            // Keep alive
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }

        } catch (MqttException e) {
            System.err.println("[Assistenza] Errore MQTT: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (pingTimer != null) pingTimer.cancel();
        }
    }

    /**
     * Ping periodico al ServerREST per heartbeat.
     */
    private void startPing() {
        pingTimer = new Timer("assistenza-ping", true);
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!guasto) {
                    publishToServer(TopicUtils.MSG_PING);
                }
            }
        }, 5000, 30_000);
    }

    /**
     * Gestisce messaggi MQTT in arrivo.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[Assistenza] Messaggio da " + topic + ": " + payload);

        // == Messaggi da Cassa ==
        if (topic.equals(TopicUtils.cassaAssistenza(idDistributore))) {
            if (payload.equals(TopicUtils.MSG_CASSA_PIENA)) {
                publishToServer(TopicUtils.MSG_CASSA_PIENA);
                disabilitaGUI();
            } else if (payload.equals(TopicUtils.MSG_GUASTO_CASSA)) {
                publishToServer(TopicUtils.MSG_GUASTO_CASSA);
                disabilitaGUI();
            }
        }

        // == Messaggi da Erogatore ==
        else if (topic.equals(TopicUtils.erogatoreAssistenza(idDistributore))) {
            if (payload.equals(TopicUtils.MSG_MANGIME_ESAURITO)) {
                publishToServer(TopicUtils.MSG_MANGIME_ESAURITO);
                disabilitaGUI();
            } else if (payload.equals(TopicUtils.MSG_GUASTO_EROGATORE)) {
                publishToServer(TopicUtils.MSG_GUASTO_EROGATORE);
                disabilitaGUI();
            }
        }

        // == Messaggi da InterfacciaUtente ==
        else if (topic.equals(TopicUtils.interfacciaUtenteAssistenza(idDistributore))) {
            if (payload.equals(TopicUtils.MSG_GUASTO_INTERFACCIA)) {
                publishToServer(TopicUtils.MSG_GUASTO_INTERFACCIA);
                disabilitaGUI();
            }
        }

        // == Risposta dal ServerREST (dopo riparazione tecnico) ==
        else if (topic.equals(TopicUtils.serverRestAssistenza(idDistributore))) {
            if (payload.contains(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA)) {
                guasto = false;
                // Notifica tutti i componenti della riparazione
                publishTo(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA,
                          TopicUtils.assistenzaInterfaccia(idDistributore));
                publishTo("cassa svuotata",
                          TopicUtils.assistenzaCassa(idDistributore));
                publishTo("mangime ricaricato",
                          TopicUtils.assistenzaErogatore(idDistributore));
                abilitaGUI();
            } else if (payload.equals(TopicUtils.MSG_ABILITA_GUI)) {
                abilitaGUI();
            } else if (payload.equals(TopicUtils.MSG_DISABILITA_GUI)) {
                disabilitaGUI();
            }
        }
    }

    private void publishToServer(String messaggio) {
        publishTo(messaggio, TopicUtils.assistenzaServerRest(idDistributore));
    }

    private void abilitaGUI() {
        publishTo(TopicUtils.MSG_ABILITA_GUI, TopicUtils.assistenzaInterfaccia(idDistributore));
    }

    private void disabilitaGUI() {
        guasto = true;
        publishTo(TopicUtils.MSG_DISABILITA_GUI, TopicUtils.assistenzaInterfaccia(idDistributore));
    }

    private void publishTo(String messaggio, String topic) {
        try {
            mqttClient.publish(topic, new MqttMessage(messaggio.getBytes()));
        } catch (MqttException e) {
            System.err.println("[Assistenza] Errore pubblicazione: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Assistenza] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* unused */ }
}
