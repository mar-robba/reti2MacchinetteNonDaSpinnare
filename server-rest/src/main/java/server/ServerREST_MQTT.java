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
     * Avvia il client MQTT e si sottoscrive ai topic generati dalle macchinette.
     * Serve a stabilire la connessione con il Broker e a indicare la callback da eseguire
     * ad ogni ricezione di payload.
     * 
     * @param brokerUrl Indirizzo del broker locale MQTT (default: tcp://localhost:1883)
     * @param username Username per la connessione
     * @param password Password per la connessione (opzionale)
     */
    public void start(String brokerUrl, String username, String password) {
        try {
            // Inizializza il client con un ID client univoco: 'server-rest-mqtt'
            client = new MqttClient(brokerUrl, "server-rest-mqtt", new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // Se vero, il broker non ricorderà lo stato
            options.setAutomaticReconnect(true); // Riconnessione autonoma in caso di caduta link
            
            // Applica le credenziali se presenti
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            // Definisce l'interfaccia MqttCallback, in ascolto di segnali dal Broker
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("[ServerREST_MQTT] Connessione persa: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Viene catturato automaticamente il messaggio e inoltrato al metodo di processamento
                    String payload = new String(message.getPayload());
                    processMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            // Connessione fisica al server (es. Eclipse Mosquitto)
            client.connect(options);

            // Sottoscrivi a tutti i topic di guasto dalle macchinette ricorrendo alla wildcard '+'
            // server/macchinetta/+/guasto ascolterà ad es. l'id 1, 2, 3 ecc.
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
     * Processa i messaggi MQTT ricevuti dinamicamente dalle macchinette.
     * Estrae l'ID dallo string del topic e direziona l'aggiornamento
     * verso la routine DB corretta o verso le classi di allarme.
     * 
     * @param topic Il "canale" in cui è pervenuto il messaggio
     * @param payload Il contenuto (es "CASSA_PIENA" o stringhe formattate Json)
     */
    private void processMessage(String topic, String payload) {
        System.out.println("[ServerREST_MQTT] Messaggio: " + topic + " = " + payload);

        // Estrai l'ID della macchinetta dal topic strutturato: server/macchinetta/{id}/guasto
        String[] parts = topic.split("/");
        
        // Verifica preliminare per impedire NullPointer e disallineamenti ad un topic non corretto
        if (parts.length < 4) return;

        int idMacchinetta;
        try {
            // Conversione base-10 del valore in array (ovvero la {id})
            idMacchinetta = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.err.println("[ServerREST_MQTT] ID macchinetta non valido: " + parts[2]);
            return;
        }

        // Smistamento Switch/If basato sui pattern terminali del topic
        if (topic.endsWith("/guasto")) {
            handleGuasto(idMacchinetta, payload);
        } else if (topic.endsWith("/guastoRisolto")) {
            handleGuastoRisolto(idMacchinetta, payload);
        } else if (topic.endsWith("/stato")) {
            System.out.println("[ServerREST_MQTT] Stato ricevuto da macchinetta " + idMacchinetta);
        }
    }

    /**
     * Gestisce e salva sul DB una segnalazione critica di guasto giunta.
     */
    private void handleGuasto(int idMacchinetta, String tipoGuasto) {
        System.out.println("[ServerREST_MQTT] Guasto da macchinetta " + idMacchinetta + ": " + tipoGuasto);

        // Aggiorna specifici flag d'allarme nelle colonne boolean DB della specifica macchinetta
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
            // In caso di guasto hardware grave forziamo l'inattività booleana 'falsa'
            dbManager.aggiornaFlagMacchinetta(idMacchinetta, "stato", false); // Imposta stato GUASTO
        }

        // Genera automaticamente un ticket pendente per i tecnici (visibile dalla WebApp)
        dbManager.creaRichiestaTecnico(idMacchinetta, tipoGuasto, "Segnalazione automatica MQTT");
    }

    /**
     * Gestisce la ripresa dell'esercizio a seguito della risoluzione del guasto.
     * Solitamente pervenuto quando un tecnico resetta la macchinetta.
     */
    private void handleGuastoRisolto(int idMacchinetta, String tipoGuasto) {
        System.out.println("[ServerREST_MQTT] Guasto risolto per macchinetta " + idMacchinetta + ": " + tipoGuasto);

        // Annulla il flag 'true' commutandolo in 'false' nel record DB relazionale
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
