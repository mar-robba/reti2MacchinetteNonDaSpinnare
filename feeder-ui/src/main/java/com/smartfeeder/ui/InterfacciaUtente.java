package com.smartfeeder.ui;

import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.common.TopicUtils;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Microservizio InterfacciaUtente.
 * Gestisce la logica MQTT per lo schermino del distributore.
 * Tre stati: "Inserisci moneta", "Erogazione", "Fuori Servizio".
 */
public class InterfacciaUtente implements MqttCallback {

    private final int idDistributore;
    private final String mqttUsername;
    private final String mqttPassword;
    private MqttClient mqttClient;

    private InterfacciaUtenteGUI gui;
    private volatile boolean guiAbilitata = true;

    public InterfacciaUtente(int idDistributore, String password) {
        this.idDistributore = idDistributore;
        this.mqttUsername = TopicUtils.INTERFACCIA_UTENTE + idDistributore;
        this.mqttPassword = password;
    }

    /**
     * Avvia la GUI e il bridge MQTT.
     */
    public void avvia() {
        // Avvia GUI Swing nel thread EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            gui = new InterfacciaUtenteGUI(idDistributore, this);
            gui.setVisible(true);
        });

        // Connetti al broker
        try {
            mqttClient = MqttClientFactory.createAndConnect(
                    mqttUsername, "smartfeeder", mqttPassword, this);

            // Ascolta messaggi dalla Cassa
            mqttClient.subscribe(TopicUtils.cassaInterfaccia(idDistributore), 1);
            // Ascolta messaggi dall'Assistenza
            mqttClient.subscribe(TopicUtils.assistenzaInterfaccia(idDistributore), 1);
            // Ascolta messaggi dall'Erogatore
            mqttClient.subscribe(TopicUtils.erogatoreInterfaccia(idDistributore), 1);

            System.out.println("[InterfacciaUtente] Avviata. ID distributore: " + idDistributore);

        } catch (MqttException e) {
            System.err.println("[InterfacciaUtente] Errore MQTT: " + e.getMessage());
            invioGuasto();
        }
    }

    /**
     * Invia messaggio "moneta inserita" alla Cassa.
     */
    public void inviaMonetaInserita() {
        if (!guiAbilitata) return;
        publishTo(TopicUtils.MSG_INSERISCI_MONETA, TopicUtils.interfacciaUtenteCassa(idDistributore));
    }

    /**
     * Invia segnalazione guasto all'Assistenza.
     */
    public void invioGuasto() {
        if (gui != null) gui.disabilitaGUI();
        publishTo(TopicUtils.MSG_GUASTO_INTERFACCIA,
                  TopicUtils.interfacciaUtenteAssistenza(idDistributore));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[InterfacciaUtente] Messaggio: " + payload);

        if (gui == null) return;

        // Dalla Cassa: moneta accettata
        if (payload.equals("Moneta accettata")) {
            gui.aggiornaSchermo("Erogazione in corso...");
        }
        // Dall'Erogatore: erogazione completata
        else if (payload.equals(TopicUtils.MSG_EROGAZIONE_COMPLETATA)) {
            gui.aggiornaSchermo("Erogazione completata!");
            // Dopo 3 secondi, torna allo stato iniziale
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                if (guiAbilitata && gui != null) {
                    gui.aggiornaSchermo("Inserisci moneta");
                }
            }).start();
        }
        // Dall'Assistenza: disabilita GUI (fuori servizio)
        else if (payload.equals(TopicUtils.MSG_DISABILITA_GUI)) {
            guiAbilitata = false;
            gui.disabilitaGUI();
        }
        // Dall'Assistenza: riabilita GUI
        else if (payload.equals(TopicUtils.MSG_ABILITA_GUI)
                || payload.contains(TopicUtils.MSG_RIPARAZIONE_EFFETTUATA)) {
            guiAbilitata = true;
            gui.abilitaGUI();
        }
    }

    private void publishTo(String messaggio, String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topic, new MqttMessage(messaggio.getBytes()));
            }
        } catch (MqttException e) {
            System.err.println("[InterfacciaUtente] Errore pubblicazione: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[InterfacciaUtente] Connessione persa.");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* unused */ }
}
