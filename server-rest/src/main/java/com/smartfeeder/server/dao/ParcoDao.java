package com.smartfeeder.server.dao;

import com.smartfeeder.server.config.DatabaseManager;
import com.smartfeeder.server.model.Parco;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per accesso alla tabella parchi.
 */
public class ParcoDao {

    public List<Parco> findAll() {
        List<Parco> parchi = new ArrayList<>();
        String sql = "SELECT * FROM parchi ORDER BY nome";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                parchi.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ParcoDao] Errore findAll: " + e.getMessage());
        }
        return parchi;
    }

    public Parco findById(int id) {
        String sql = "SELECT * FROM parchi WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ParcoDao] Errore findById: " + e.getMessage());
        }
        return null;
    }

    public Parco create(Parco parco) {
        String sql = "INSERT INTO parchi (nome, indirizzo, citta) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, parco.getNome());
            ps.setString(2, parco.getIndirizzo());
            ps.setString(3, parco.getCitta());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) parco.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[ParcoDao] Errore create: " + e.getMessage());
        }
        return parco;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM parchi WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ParcoDao] Errore delete: " + e.getMessage());
        }
        return false;
    }

    private Parco mapRow(ResultSet rs) throws SQLException {
        return new Parco(
            rs.getInt("id"),
            rs.getString("nome"),
            rs.getString("indirizzo"),
            rs.getString("citta")
        );
    }
}
