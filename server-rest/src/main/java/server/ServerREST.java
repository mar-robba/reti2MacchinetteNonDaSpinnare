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
        // Configurazione della porta in ascolto per il server Spark
        port(PORT);

        // Inizializza il manager per la connessione e le query al database MySQL
        dbManager = new DatabaseManager();

        // Inizializza il bridge MQTT passando il riferimento al database
        mqttBridge = new ServerREST_MQTT(dbManager);
        // Avvia la connessione al broker locale sulla porta di default 1883
        mqttBridge.start("tcp://localhost:1883", "server", "serverpass");

        // Configurazione dei filtri CORS (Cross-Origin Resource Sharing)
        // Permette alla web app (che gira su una porta o dominio diverso) di effettuare richieste alle API
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            // Imposta il tipo di contenuto della risposta in JSON
            response.type("application/json");
        });

        // Risposta generica per la pre-flight request (richiesta OPTIONS) lanciata dal browser
        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        // =================== SCUOLE ===================

        // GET /api/scuole – Ritorna la lista JSON degli istituti (scuole) registrati (pag.41)
        get("/api/scuole", (req, res) -> {
            return dbManager.getScuole();
        }, gson::toJson);

        // POST /api/scuole – Registra una nuova scuola nel sistema (pag.36)
        post("/api/scuole", (req, res) -> {
            // Parsifica il body JSON ricevuto in un oggetto
            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            
            // Estrazione sicura dei campi dal JSON (se mancano, diventano null)
            String nome = getStringOrNull(body, "nome");
            String indirizzo = getStringOrNull(body, "indirizzo");
            String citta = getStringOrNull(body, "citta");
            String provincia = getStringOrNull(body, "provincia");
            String cap = getStringOrNull(body, "cap");

            // Validazione essenziale (pag.20: MostraErrore("Dati non validi"))
            // Il nome della scuola è un parametro obbligatorio per il salvataggio
            if (nome == null || nome.trim().isEmpty()) {
                res.status(400); // 400 Bad Request
                return errorJson("Dati non validi: il nome è obbligatorio");
            }

            // Inserisce la scuola sul DB; ritorna -1 in caso di nome già registrato
            int id = dbManager.aggiungiScuola(nome, indirizzo, citta, provincia, cap);
            if (id == -1) {
                res.status(409); // 409 Conflict
                return errorJson("Scuola già esistente");
            }

            // Scuola aggiunta con successo
            res.status(201); // 201 Created
            JsonObject result = new JsonObject();
            result.addProperty("id", id);
            result.addProperty("messaggio", "Scuola aggiunta con successo");
            return result;
        }, gson::toJson);

        // DELETE /api/scuole/:id – Rimuove una scuola, comportando l'eliminazione a cascata (CASCADE) delle macchinette associate (pag.38)
        delete("/api/scuole/:id", (req, res) -> {
            // Legge il parametro 'id' dall'URL della richiesta HTTP
            int id = Integer.parseInt(req.params(":id"));

            // Verifica che la scuola esista per evitare modifiche su stato inesistente
            if (!dbManager.scuolaEsiste(id)) {
                res.status(404);
                return errorJson("Scuola non trovata");
            }

            boolean eliminata = dbManager.eliminaScuola(id);
            if (eliminata) {
                        // cosa è queto successJson ?
                return successJson("Scuola eliminata con tutte le macchinette");
            }
            res.status(500);
            return errorJson("Errore durante l'eliminazione");
            // parsifica in jason il return è una callback
        }, gson::toJson);

        // =================== MACCHINETTE ===================

        // GET /api/macchinette – Visualizza l'inventario completo di tutte le macchinette registrate nel sistema (pag.42)
        get("/api/macchinette", (req, res) -> {
            return dbManager.getMacchinette();
        }, gson::toJson);

        // GET /api/scuole/:id/macchinette – Recupera unicamente la lista delle macchinette collegate ad un plesso scolastico specifico (pag.41)
        get("/api/scuole/:id/macchinette", (req, res) -> {
            int idScuola = Integer.parseInt(req.params(":id"));
            // Verifica se l'ID referenziato nello URL ha una controparte nel DB
            if (!dbManager.scuolaEsiste(idScuola)) {
                res.status(404); // 404 Not Found
                return errorJson("Scuola non trovata");
            }
            return dbManager.getMacchinetteByScuola(idScuola);
        }, gson::toJson);

        // POST /api/macchinette – Installa/Registra una nuova macchinetta in una determinata scuola (pag.37)
        post("/api/macchinette", (req, res) -> {
            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            // Recupera l'ID della scuola dal body del JSON. Di default è -1 se non impostato.
            int idScuola = body.has("id_scuola") ? body.get("id_scuola").getAsInt() : -1;
            String nome = getStringOrNull(body, "nome");

            // Validazione essenziale: sia id_scuola sia nome della macchinetta sono obbligatori
            if (idScuola <= 0 || nome == null || nome.trim().isEmpty()) {
                res.status(400);
                return errorJson("Errore nella compilazione del form");
            }

            // Verifica l'integrità referenziale, confermando che la scuola per l'associazione esista
            if (!dbManager.scuolaEsiste(idScuola)) {
                res.status(404);
                return errorJson("Scuola non trovata. Creare prima la scuola.");
            }

            // Tenta l'inserimento sul database MySQL MySQL
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

        // GET /api/macchinette/:id/stato – Permette di scaricare lo stato real-time e l'elenco dei flag di allarmi della macchinetta (pag.43)
        get("/api/macchinette/:id/stato", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            JsonObject stato = dbManager.getStatoMacchinetta(id);
            if (stato == null) {
                res.status(404); // 404 Not Found se il distributore non esiste
                return errorJson("Macchinetta non trovata");
            }
            return stato;
        }, gson::toJson);

        // POST /api/macchinette/:id/invia-tecnico – Sollecita manualmente ed esplicitamente l'intervento tecnico (pag.43)
        post("/api/macchinette/:id/invia-tecnico", (req, res) -> {
            int idMacchinetta = Integer.parseInt(req.params(":id"));

            // Pre-verifica necessaria prima di generare un ticket inutile
            if (!dbManager.macchinettaEsiste(idMacchinetta)) {
                res.status(404);
                return errorJson("Macchinetta non trovata");
            }

            JsonObject body = gson.fromJson(req.body(), JsonObject.class);
            String tipoGuasto = getStringOrNull(body, "tipo_guasto");
            // Se nessun tipo di guasto viene inviato assieme alla request, assume "GUASTO_GENERICO"
            if (tipoGuasto == null) tipoGuasto = "GUASTO_GENERICO";

            // Creazione stringa descrittiva e salvataggio del ticket formale nel db
            int idRichiesta = dbManager.creaRichiestaTecnico(idMacchinetta, tipoGuasto, "Tecnico inviato dall'operatore");

            // Elemento fondamentale: notifica la specifica macchinetta in maniera sincrona via MQTT del preposto arrivo del tecnico
            mqttBridge.publishToMacchinetta(idMacchinetta, "assistenza/tecnicoInviato", tipoGuasto);

            JsonObject result = new JsonObject();
            result.addProperty("id_richiesta", idRichiesta);
            result.addProperty("messaggio", "Tecnico inviato con successo");
            res.status(201);
            return result;
        }, gson::toJson);

        // =================== RICHIESTE TECNICO ===================

        // GET /api/richieste – Recupera l'elenco delle richieste di assistenza aperte per il tecnico (pag.44)
        get("/api/richieste", (req, res) -> {
            return dbManager.getRichiesteTecnico();
        }, gson::toJson);

        // DELETE /api/richieste/:id – Segna una richiesta come COMPLETATA invece di rimuoverla fisicamente dal DB (soft-delete logico) (pag.44)
        delete("/api/richieste/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            
            // Effettua la query di update
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
