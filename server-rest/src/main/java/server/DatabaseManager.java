package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * DatabaseManager – Gestione CRUD del database MySQL per il sistema PISSIR.
 * Utilizzato dal ServerREST per interagire con il database centralizzato.
 */
public class DatabaseManager {

    private String url;
    private String username;
    private String password;

    public DatabaseManager(String host, int port, String dbName, String username, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.username = username;
        this.password = password;
    }

    public DatabaseManager() {
        this("localhost", 3306, "pissir_db", "pissir_user", "pissir_password");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // =================== SCUOLE ===================

    /**
     * Restituisce l'elenco di tutte le scuole.
     * Recupera tutte le righe dalla tabella 'scuole' ordinandole alfabeticamente.
     */
    public JsonArray getScuole() {
        JsonArray result = new JsonArray();
        // Try-with-resources per auto-chiusura in sicurezza degli statement JDBC
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM scuole ORDER BY nome")) {
            // Itera sui record forniti della query
            while (rs.next()) {
                JsonObject scuola = new JsonObject();
                scuola.addProperty("id", rs.getInt("id"));
                scuola.addProperty("nome", rs.getString("nome"));
                scuola.addProperty("indirizzo", rs.getString("indirizzo"));
                scuola.addProperty("citta", rs.getString("citta"));
                scuola.addProperty("provincia", rs.getString("provincia"));
                scuola.addProperty("cap", rs.getString("cap"));
                result.add(scuola);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getScuole: " + e.getMessage());
        }
        return result;
    }

    /**
     * Aggiunge una nuova scuola. Restituisce l'ID generato dall'AUTO_INCREMENT, oppure -1 se esiste già.
     */
    public int aggiungiScuola(String nome, String indirizzo, String citta, String provincia, String cap) {
        // Controlla preliminarmente se esiste già una scuola omonima (evita doppioni)
        try (Connection conn = getConnection();
             PreparedStatement check = conn.prepareStatement("SELECT id FROM scuole WHERE nome = ?")) {
            check.setString(1, nome);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return -1; // Scuola già esistente, rompe l'esecuzione e ritorna -1
            }

            // Inserisce record e preleva chiave (ID) per poterla ritornare al frontend (o chiamante REST)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scuole (nome, indirizzo, citta, provincia, cap) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nome);
                ps.setString(2, indirizzo);
                ps.setString(3, citta);
                ps.setString(4, provincia);
                ps.setString(5, cap);
                ps.executeUpdate();

                // Recupera l'ID univoco auto-generato dal backend del database
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore aggiungiScuola: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Elimina una scuola per ID (CASCADE eliminerà anche le macchinette).
     */
    public boolean eliminaScuola(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM scuole WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Errore eliminaScuola: " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica se una scuola esiste.
     */
    public boolean scuolaEsiste(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM scuole WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore scuolaEsiste: " + e.getMessage());
        }
        return false;
    }

    // =================== MACCHINETTE ===================

    /**
     * Restituisce l'elenco di tutte le macchinette formattato in Array JSON.
     * Effettua una JOIN SQL con la tabella Scuole per risolvere la foreign key ottenendo nomi in un colpo solo.
     */
    public JsonArray getMacchinette() {
        JsonArray result = new JsonArray();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT m.*, s.nome AS nome_scuola FROM macchinette m " +
                     "JOIN scuole s ON m.id_scuola = s.id ORDER BY m.id")) {
            while (rs.next()) {
                // Utilizza un helper privato serializzando su JsonObject
                result.add(macchinettaFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getMacchinette: " + e.getMessage());
        }
        return result;
    }

    /**
     * Restituisce le macchinette di una specifica scuola.
     */
    public JsonArray getMacchinetteByScuola(int idScuola) {
        JsonArray result = new JsonArray();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT m.*, s.nome AS nome_scuola FROM macchinette m " +
                     "JOIN scuole s ON m.id_scuola = s.id WHERE m.id_scuola = ? ORDER BY m.id")) {
            ps.setInt(1, idScuola);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(macchinettaFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getMacchinetteByScuola: " + e.getMessage());
        }
        return result;
    }

    /**
     * Restituisce lo stato di una macchinetta specifica.
     */
    public JsonObject getStatoMacchinetta(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT m.*, s.nome AS nome_scuola FROM macchinette m " +
                     "JOIN scuole s ON m.id_scuola = s.id WHERE m.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return macchinettaFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getStatoMacchinetta: " + e.getMessage());
        }
        return null;
    }

    /**
     * Aggiunge una nuova macchinetta a una scuola.
     */
    public int aggiungiMacchinetta(int idScuola, String nome) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO macchinette (id_scuola, nome) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idScuola);
            ps.setString(2, nome);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore aggiungiMacchinetta: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Elimina una macchinetta per ID.
     */
    public boolean eliminaMacchinetta(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM macchinette WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Errore eliminaMacchinetta: " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica se una macchinetta esiste.
     */
    public boolean macchinettaEsiste(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM macchinette WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore macchinettaEsiste: " + e.getMessage());
        }
        return false;
    }

    /**
     * Aggiorna a runtime e parametricamente i flag di stato allarme e magazzino di una data macchinetta.
     * Adattabile, usato massicciamente da MQTT Bridge.
     */
    public void aggiornaFlagMacchinetta(int id, String flagName, boolean value) {
        // Poichè i field di query dinamici sono anti-pattern, prestiamo attenzione alla composizione della stringa per evitare SQL Injection
        String sql = "UPDATE macchinette SET " + flagName + " = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Errore aggiornaFlag: " + e.getMessage());
        }
    }

    /**
     * Restituisce gli ID delle macchinette di una scuola.
     */
    public List<Integer> getIdMacchinetteByScuola(int idScuola) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM macchinette WHERE id_scuola = ?")) {
            ps.setInt(1, idScuola);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getIdMacchinette: " + e.getMessage());
        }
        return ids;
    }

    // =================== RICHIESTE TECNICO ===================

    /**
     * Crea una nuova richiesta per il tecnico.
     */
    public int creaRichiestaTecnico(int idMacchinetta, String tipoGuasto, String descrizione) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO richieste_tecnico (id_macchinetta, tipo_guasto, descrizione) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idMacchinetta);
            ps.setString(2, tipoGuasto);
            ps.setString(3, descrizione);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore creaRichiesta: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Restituisce l'elenco delle richieste di urgenza in corso (da eseguire) per il tecnico.
     * Viene usato l'operatore JOIN per legare i nomi degli istituti all'elenco dei ticket delle dipendenti macchinette.
     */
    public JsonArray getRichiesteTecnico() {
        JsonArray result = new JsonArray();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT r.*, m.nome AS nome_macchinetta, s.nome AS nome_scuola " +
                     "FROM richieste_tecnico r " +
                     "JOIN macchinette m ON r.id_macchinetta = m.id " +
                     "JOIN scuole s ON m.id_scuola = s.id " +
                     "WHERE r.stato != 'COMPLETATA' ORDER BY r.data_apertura DESC")) {
            while (rs.next()) {
                JsonObject richiesta = new JsonObject();
                // Popolamento manuale della striscia JSON
                richiesta.addProperty("id", rs.getInt("id"));
                richiesta.addProperty("id_macchinetta", rs.getInt("id_macchinetta"));
                richiesta.addProperty("nome_macchinetta", rs.getString("nome_macchinetta"));
                richiesta.addProperty("nome_scuola", rs.getString("nome_scuola"));
                richiesta.addProperty("tipo_guasto", rs.getString("tipo_guasto"));
                richiesta.addProperty("descrizione", rs.getString("descrizione"));
                richiesta.addProperty("stato", rs.getString("stato"));
                richiesta.addProperty("data_apertura", rs.getTimestamp("data_apertura").toString());
                result.add(richiesta);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getRichieste: " + e.getMessage());
        }
        return result;
    }

    /**
     * Elimina (completa) una richiesta tecnico.
     */
    public boolean eliminaRichiestaTecnico(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE richieste_tecnico SET stato = 'COMPLETATA', data_chiusura = NOW() WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Errore eliminaRichiesta: " + e.getMessage());
        }
        return false;
    }

    // =================== HELPER ===================

    private JsonObject macchinettaFromResultSet(ResultSet rs) throws SQLException {
        JsonObject m = new JsonObject();
        m.addProperty("id", rs.getInt("id"));
        m.addProperty("id_scuola", rs.getInt("id_scuola"));
        m.addProperty("nome_scuola", rs.getString("nome_scuola"));
        m.addProperty("nome", rs.getString("nome"));
        m.addProperty("stato", rs.getString("stato"));
        m.addProperty("cassa_totale", rs.getDouble("cassa_totale"));
        m.addProperty("flag_cassa_piena", rs.getBoolean("flag_cassa_piena"));
        m.addProperty("flag_cialde_esaurimento", rs.getBoolean("flag_cialde_esaurimento"));
        m.addProperty("flag_zucchero_esaurimento", rs.getBoolean("flag_zucchero_esaurimento"));
        m.addProperty("flag_bicchieri_esaurimento", rs.getBoolean("flag_bicchieri_esaurimento"));
        m.addProperty("flag_guasto_generico", rs.getBoolean("flag_guasto_generico"));
        return m;
    }
}
