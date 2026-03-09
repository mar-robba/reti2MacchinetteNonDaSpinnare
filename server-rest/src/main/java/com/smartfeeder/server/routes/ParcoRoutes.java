package com.smartfeeder.server.routes;

import com.google.gson.Gson;
import com.smartfeeder.server.dao.ParcoDao;
import com.smartfeeder.server.model.Parco;

import static spark.Spark.*;

/**
 * Rotte REST per la gestione dei Parchi.
 */
public class ParcoRoutes {

    private static final Gson gson = com.smartfeeder.server.utils.GsonUtils.GSON;
    private static final ParcoDao dao = new ParcoDao();

    public static void register() {
        // GET /api/parchi — elenco di tutti i parchi
        get("/api/parchi", (req, res) -> {
            res.type("application/json");
            return gson.toJson(dao.findAll());
        });

        // GET /api/parchi/:id — dettaglio singolo parco
        get("/api/parchi/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Parco parco = dao.findById(id);
            if (parco == null) {
                res.status(404);
                return "{\"error\": \"Parco non trovato\"}";
            }
            res.type("application/json");
            return gson.toJson(parco);
        });

        // POST /api/parchi — crea nuovo parco
        post("/api/parchi", (req, res) -> {
            Parco parco = gson.fromJson(req.body(), Parco.class);
            parco = dao.create(parco);
            res.status(201);
            res.type("application/json");
            return gson.toJson(parco);
        });

        // DELETE /api/parchi/:id — elimina parco
        delete("/api/parchi/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            if (dao.delete(id)) {
                res.status(200);
                return "{\"message\": \"Parco eliminato\"}";
            }
            res.status(404);
            return "{\"error\": \"Parco non trovato\"}";
        });
    }
}
