package com.smartfeeder.server;

import com.smartfeeder.server.config.DatabaseManager;
import com.smartfeeder.server.mqtt.MqttMessageHandler;
import com.smartfeeder.server.routes.DistributoreRoutes;
import com.smartfeeder.server.routes.ParcoRoutes;
import com.smartfeeder.server.routes.TicketRoutes;

import static spark.Spark.*;

/**
 * Entry point del Server REST (SparkJava).
 * Espone endpoint RESTful per la gestione di Parchi, Distributori e Ticket Guasti.
 */
public class ServerRestApplication {

    public static void main(String[] args) {
        // Porta del server
        int porta = 8081;
        if (args.length > 0) {
            try { porta = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { /* usa default */ }
        }
        port(porta);

        // CORS per la Web App
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            return "OK";
        });

        // Inizializza Database
        DatabaseManager.initialize();

        // Avvia il listener MQTT per i messaggi dall'edge
        MqttMessageHandler mqttHandler = new MqttMessageHandler();
        mqttHandler.start();

        // Registra le rotte API
        ParcoRoutes.register();
        DistributoreRoutes.register();
        TicketRoutes.register(mqttHandler);



        System.out.println("[ServerREST] Avviato sulla porta " + porta);

        // Gestione risposta JSON di default
        after((req, res) -> {
            if (res.type() == null) {
                res.type("application/json");
            }
        });

        // Gestione errori
        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
            System.err.println("[ServerREST] Errore: " + e.getMessage());
        });

        notFound((req, res) -> {
            res.type("application/json");
            return "{\"error\": \"Endpoint non trovato\"}";
        });
    }
}
