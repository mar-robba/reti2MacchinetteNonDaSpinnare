package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static spark.Spark.*;

/**
 * ServerREST – Server API RESTful con Apache Spark (Spark Java).
 * Come da diagramma dei componenti pagina 4 e diagrammi di sequenza pagg.35-44.
 *
 * Espone endpoint REST per la web app gestionale e integra MQTT
 * per la comunicazione con le macchinette.
 *
 * Endpoint:
 *   GET    /api/scuole                    - Elenco istituti
 *   POST   /api/scuole                    - Aggiungi scuola
 *   DELETE /api/scuole/:id                - Elimina scuola (+ tutte le macchinette)
 *   GET    /api/macchinette               - Elenco tutte macchinette
 *   GET    /api/scuole/:id/macchinette    - Macchinette di una scuola
 *   POST   /api/macchinette               - Aggiungi macchinetta
 *   DELETE /api/macchinette/:id           - Elimina macchinetta
 *   GET    /api/macchinette/:id/stato     - Stato macchinetta
 *   POST   /api/macchinette/:id/invia-tecnico - Invia tecnico
 *   GET    /api/richieste                 - Elenco richieste tecnico
 *   DELETE /api/richieste/:id             - Elimina (completa) richiesta
 */
public class ServerREST {

    private static final int PORT = 8081;
    private static final Gson gson = new Gson();
    private static DatabaseManager dbManager;
    private static ServerREST_MQTT mqttBridge;

    public static void main(String[] args) {
        // Configurazione
        port(PORT);

        // Inizializza database manager
        dbManager = new DatabaseManager();

        // Inizializza bridge MQTT
        mqttBridge = new ServerREST_MQTT(dbManager);
        mqttBridge.start("tcp://localhost:1883", "server", "serverpass");

        // CORS per la web app
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.type("application/json");
        });

        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        // =================== SCUOLE ===================

        // GET /api/scuole – Elenco istituti (pag.41)
        get("/api/scuole", (req, res) -> {
            return dbManager.getScuole();
        }, gson::toJson);

        // POST /api/scuole – Aggiungi scuola (pag.36)
        post("/api/scuole", (req, res) -> {
            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            String nome = getStringOrNull(body, "nome");
            String indirizzo = getStringOrNull(body, "indirizzo");
            String citta = getStringOrNull(body, "citta");
            String provincia = getStringOrNull(body, "provincia");
            String cap = getStringOrNull(body, "cap");

            // Validazione (pag.20: MostraErrore("Dati non validi"))
            if (nome == null || nome.trim().isEmpty()) {
                res.status(400);
                return errorJson("Dati non validi: il nome è obbligatorio");
            }

            int id = dbManager.aggiungiScuola(nome, indirizzo, citta, provincia, cap);
            if (id == -1) {
                res.status(409);
                return errorJson("Scuola già esistente");
            }

            res.status(201);
            JsonObject result = new JsonObject();
            result.addProperty("id", id);
            result.addProperty("messaggio", "Scuola aggiunta con successo");
            return result;
        }, gson::toJson);

        // DELETE /api/scuole/:id – Elimina scuola (pag.38)
        delete("/api/scuole/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));

            if (!dbManager.scuolaEsiste(id)) {
                res.status(404);
                return errorJson("Scuola non trovata");
            }

            boolean eliminata = dbManager.eliminaScuola(id);
            if (eliminata) {
                return successJson("Scuola eliminata con tutte le macchinette");
            }
            res.status(500);
            return errorJson("Errore durante l'eliminazione");
        }, gson::toJson);

        // =================== MACCHINETTE ===================

        // GET /api/macchinette – Elenco tutte macchinette (pag.42)
        get("/api/macchinette", (req, res) -> {
            return dbManager.getMacchinette();
        }, gson::toJson);

        // GET /api/scuole/:id/macchinette – Macchinette di una scuola (pag.41)
        get("/api/scuole/:id/macchinette", (req, res) -> {
            int idScuola = Integer.parseInt(req.params(":id"));
            if (!dbManager.scuolaEsiste(idScuola)) {
                res.status(404);
                return errorJson("Scuola non trovata");
            }
            return dbManager.getMacchinetteByScuola(idScuola);
        }, gson::toJson);

        // POST /api/macchinette – Aggiungi macchinetta (pag.37)
        post("/api/macchinette", (req, res) -> {
            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            int idScuola = body.has("id_scuola") ? body.get("id_scuola").getAsInt() : -1;
            String nome = getStringOrNull(body, "nome");

            if (idScuola <= 0 || nome == null || nome.trim().isEmpty()) {
                res.status(400);
                return errorJson("Errore nella compilazione del form");
            }

            if (!dbManager.scuolaEsiste(idScuola)) {
                res.status(404);
                return errorJson("Scuola non trovata. Creare prima la scuola.");
            }

            int id = dbManager.aggiungiMacchinetta(idScuola, nome);
            if (id == -1) {
                res.status(500);
                return errorJson("Errore durante l'aggiunta della macchinetta");
            }

            res.status(201);
            JsonObject result = new JsonObject();
            result.addProperty("id", id);
            result.addProperty("messaggio", "Macchinetta aggiunta con successo");
            return result;
        }, gson::toJson);

        // DELETE /api/macchinette/:id – Elimina macchinetta (pag.39)
        delete("/api/macchinette/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));

            if (!dbManager.macchinettaEsiste(id)) {
                res.status(404);
                return errorJson("Macchinetta non trovata");
            }

            boolean eliminata = dbManager.eliminaMacchinetta(id);
            if (eliminata) {
                return successJson("Macchinetta eliminata dall'elenco");
            }
            res.status(500);
            return errorJson("Errore durante l'eliminazione");
        }, gson::toJson);

        // GET /api/macchinette/:id/stato – Stato macchinetta (pag.43)
        get("/api/macchinette/:id/stato", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            JsonObject stato = dbManager.getStatoMacchinetta(id);
            if (stato == null) {
                res.status(404);
                return errorJson("Macchinetta non trovata");
            }
            return stato;
        }, gson::toJson);

        // POST /api/macchinette/:id/invia-tecnico – Invia tecnico (pag.43)
        post("/api/macchinette/:id/invia-tecnico", (req, res) -> {
            int idMacchinetta = Integer.parseInt(req.params(":id"));

            if (!dbManager.macchinettaEsiste(idMacchinetta)) {
                res.status(404);
                return errorJson("Macchinetta non trovata");
            }

            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            String tipoGuasto = getStringOrNull(body, "tipo_guasto");
            if (tipoGuasto == null) tipoGuasto = "GUASTO_GENERICO";

            int idRichiesta = dbManager.creaRichiestaTecnico(idMacchinetta, tipoGuasto, "Tecnico inviato dall'operatore");

            // Notifica la macchinetta via MQTT
            mqttBridge.publishToMacchinetta(idMacchinetta, "assistenza/tecnicoInviato", tipoGuasto);

            JsonObject result = new JsonObject();
            result.addProperty("id_richiesta", idRichiesta);
            result.addProperty("messaggio", "Tecnico inviato con successo");
            res.status(201);
            return result;
        }, gson::toJson);

        // =================== RICHIESTE TECNICO ===================

        // GET /api/richieste – Elenco richieste tecnico (pag.44)
        get("/api/richieste", (req, res) -> {
            return dbManager.getRichiesteTecnico();
        }, gson::toJson);

        // DELETE /api/richieste/:id – Elimina richiesta (pag.44)
        delete("/api/richieste/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            boolean completata = dbManager.eliminaRichiestaTecnico(id);
            if (completata) {
                return successJson("Richiesta completata e chiusa");
            }
            res.status(404);
            return errorJson("Richiesta non trovata");
        }, gson::toJson);

        // =================== INFO ===================

        get("/api/info", (req, res) -> {
            JsonObject info = new JsonObject();
            info.addProperty("nome", "Server REST PISSIR");
            info.addProperty("versione", "1.0");
            info.addProperty("porta", PORT);
            return info;
        }, gson::toJson);

        System.out.println("=================================================");
        System.out.println("  Server REST PISSIR avviato sulla porta " + PORT);
        System.out.println("  Endpoint base: http://localhost:" + PORT + "/api");
        System.out.println("=================================================");
    }

    // =================== HELPER ===================

    private static String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static JsonObject errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("errore", message);
        return error;
    }

    private static JsonObject successJson(String message) {
        JsonObject success = new JsonObject();
        success.addProperty("messaggio", message);
        return success;
    }
}
