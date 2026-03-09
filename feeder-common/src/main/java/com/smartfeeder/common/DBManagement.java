package com.smartfeeder.common;

import com.google.gson.JsonObject;
import java.sql.*;

/**
 * Gestione database locale SQLite per ogni distributore.
 * Il DB locale contiene lo stato fisico del distributore:
 * monete contate, capacità cassa, mangime disponibile.
 */
public class DBManagement {

    private final String dbPath;
    private Connection connection;

    public DBManagement(int idDistributore) {
        this.dbPath = "distributore_" + idDistributore + ".db";
    }

    public DBManagement(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Apre/crea la connessione SQLite.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
        return connection;
    }

    /**
     * Legge l'intero DB locale come JsonObject.
     */
    public JsonObject readDBLocalAsJson() {
        JsonObject result = new JsonObject();
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM stato_distributore LIMIT 1")) {
                if (rs.next()) {
                    result.addProperty("monete_contate", rs.getInt("monete_contate"));
                    result.addProperty("capacita_cassa", rs.getInt("capacita_cassa"));
                    result.addProperty("mangime_disponibile", rs.getInt("mangime_disponibile"));
                    result.addProperty("guasta", rs.getInt("guasta"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore lettura DB: " + e.getMessage());
        }
        return result;
    }

    /**
     * Legge il DB locale come stringa JSON.
     */
    public String readDBLocalAsString() {
        return readDBLocalAsJson().toString();
    }

    /**
     * Aggiorna il contatore monete nel DB locale.
     */
    public void updateMoneteContate(int moneteContate) {
        String sql = "UPDATE stato_distributore SET monete_contate=? WHERE id=1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, moneteContate);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore aggiornamento monete: " + e.getMessage());
        }
    }

    /**
     * Aggiorna il mangime disponibile nel DB locale.
     */
    public void updateMangimeDisponibile(int mangimeDisponibile) {
        String sql = "UPDATE stato_distributore SET mangime_disponibile=? WHERE id=1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, mangimeDisponibile);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore aggiornamento mangime: " + e.getMessage());
        }
    }

    /**
     * Segna lo stato di guasto nel DB locale.
     */
    public void setGuasta(boolean guasta) {
        String sql = "UPDATE stato_distributore SET guasta=? WHERE id=1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, guasta ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore aggiornamento guasta: " + e.getMessage());
        }
    }

    /**
     * Legge la capacità massima della cassa.
     */
    public int getCapacitaCassa() {
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT capacita_cassa FROM stato_distributore LIMIT 1")) {
            if (rs.next()) return rs.getInt("capacita_cassa");
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore lettura capacità: " + e.getMessage());
        }
        return 50; // default
    }

    /**
     * Legge il numero di monete contate.
     */
    public int getMoneteContate() {
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT monete_contate FROM stato_distributore LIMIT 1")) {
            if (rs.next()) return rs.getInt("monete_contate");
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore lettura monete: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Legge il mangime disponibile.
     */
    public int getMangimeDisponibile() {
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT mangime_disponibile FROM stato_distributore LIMIT 1")) {
            if (rs.next()) return rs.getInt("mangime_disponibile");
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore lettura mangime: " + e.getMessage());
        }
        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            System.err.println("[DBManagement] Errore chiusura: " + e.getMessage());
        }
    }
}
