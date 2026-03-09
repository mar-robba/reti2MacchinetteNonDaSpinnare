package com.smartfeeder.server.routes;

import com.google.gson.Gson;
import com.smartfeeder.server.dao.TicketGuastoDao;
import com.smartfeeder.server.dao.DistributoreDao;
import com.smartfeeder.server.model.TicketGuasto;
import com.smartfeeder.common.TopicUtils;
import com.smartfeeder.common.MqttClientFactory;
import com.smartfeeder.server.mqtt.MqttMessageHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static spark.Spark.*;

/**
 * Rotte REST per la gestione dei Ticket Guasti.
 */
public class TicketRoutes {

    private static final Gson gson = com.smartfeeder.server.utils.GsonUtils.GSON;
    private static final TicketGuastoDao ticketDao = new TicketGuastoDao();
    private static final DistributoreDao distributoreDao = new DistributoreDao();

    public static void register(MqttMessageHandler mqttHandler) {
        // GET /api/ticket — tutti i ticket
        get("/api/ticket", (req, res) -> {
            res.type("application/json");
            String stato = req.queryParams("stato");
            if (stato != null && !stato.isEmpty()) {
                return gson.toJson(ticketDao.findByStato(stato));
            }
            return gson.toJson(ticketDao.findAll());
        });

        // GET /api/ticket/distributore/:id — ticket per distributore
        get("/api/ticket/distributore/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            res.type("application/json");
            return gson.toJson(ticketDao.findByDistributore(id));
        });

        // POST /api/ticket — crea ticket (invocato dall'Assistenza via HTTP POST)
        post("/api/ticket", (req, res) -> {
            TicketGuasto ticket = gson.fromJson(req.body(), TicketGuasto.class);
            ticket = ticketDao.create(ticket);
            // Segna distributore come guasto
            distributoreDao.updateStatoGuasta(ticket.getIdDistributore(), true);
            res.status(201);
            res.type("application/json");
            return gson.toJson(ticket);
        });

        // POST /api/ticket/:id/risolvi — tecnico segna come risolto
        post("/api/ticket/:id/risolvi", (req, res) -> {
            int ticketId = Integer.parseInt(req.params(":id"));
            TicketGuasto originalTicket = ticketDao.findById(ticketId);
            if (originalTicket != null && ticketDao.risolvi(ticketId)) {
                // Recupera informazioni del ticket per notificare via MQTT
                mqttHandler.inviaRiparazione(originalTicket.getIdDistributore());
                // Segna distributore come non guasto
                distributoreDao.updateStatoGuasta(originalTicket.getIdDistributore(), false);
                res.status(200);
                res.type("application/json");
                return "{\"message\": \"Ticket risolto\"}";
            }
            res.status(404);
            return "{\"error\": \"Ticket non trovato\"}";
        });

        // POST /api/inviaAllarme — endpoint per segnalazione allarme dall'edge
        post("/api/inviaAllarme", (req, res) -> {
            TicketGuasto ticket = gson.fromJson(req.body(), TicketGuasto.class);
            ticket = ticketDao.create(ticket);
            distributoreDao.updateStatoGuasta(ticket.getIdDistributore(), true);
            res.status(201);
            res.type("application/json");
            System.out.println("[TicketRoutes] Allarme ricevuto: " + ticket.getTipoGuasto()
                    + " per distributore " + ticket.getIdDistributore());
            return gson.toJson(ticket);
        });
    }
}
