package com.smartfeeder.server.routes;

import com.google.gson.Gson;
import com.smartfeeder.server.dao.DistributoreDao;
import com.smartfeeder.server.model.Distributore;

import static spark.Spark.*;

/**
 * Rotte REST per la gestione dei Distributori.
 */
public class DistributoreRoutes {

    private static final Gson gson = com.smartfeeder.server.utils.GsonUtils.GSON;
    private static final DistributoreDao dao = new DistributoreDao();

    public static void register() {
        // GET /api/distributori — elenco di tutti i distributori
        get("/api/distributori", (req, res) -> {
            res.type("application/json");
            return gson.toJson(dao.findAll());
        });

        // GET /api/distributori/:id — dettaglio singolo distributore
        get("/api/distributori/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Distributore d = dao.findById(id);
            if (d == null) {
                res.status(404);
                return "{\"error\": \"Distributore non trovato\"}";
            }
            res.type("application/json");
            return gson.toJson(d);
        });

        // GET /api/distributori/parco/:idParco — distributori per parco
        get("/api/distributori/parco/:idParco", (req, res) -> {
            int idParco = Integer.parseInt(req.params(":idParco"));
            res.type("application/json");
            return gson.toJson(dao.findByParco(idParco));
        });

        // POST /api/distributori — crea nuovo distributore
        post("/api/distributori", (req, res) -> {
            Distributore d = gson.fromJson(req.body(), Distributore.class);
            d = dao.create(d);
            res.status(201);
            res.type("application/json");
            return gson.toJson(d);
        });

        // DELETE /api/distributori/:id — elimina distributore
        delete("/api/distributori/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            if (dao.delete(id)) {
                res.status(200);
                return "{\"message\": \"Distributore eliminato\"}";
            }
            res.status(404);
            return "{\"error\": \"Distributore non trovato\"}";
        });
    }
}
