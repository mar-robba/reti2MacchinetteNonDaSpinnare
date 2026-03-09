package com.smartfeeder.server.dao;

import com.smartfeeder.server.config.DatabaseManager;
import com.smartfeeder.server.model.Distributore;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per accesso alla tabella distributori.
 */
public class DistributoreDao {

    public List<Distributore> findAll() {
        List<Distributore> list = new ArrayList<>();
        String sql = "SELECT d.*, p.nome AS nome_parco FROM distributori d " +
                     "JOIN parchi p ON d.id_parco = p.id ORDER BY d.id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore findAll: " + e.getMessage());
        }
        return list;
    }

    public Distributore findById(int id) {
        String sql = "SELECT d.*, p.nome AS nome_parco FROM distributori d " +
                     "JOIN parchi p ON d.id_parco = p.id WHERE d.id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore findById: " + e.getMessage());
        }
        return null;
    }

    public List<Distributore> findByParco(int idParco) {
        List<Distributore> list = new ArrayList<>();
        String sql = "SELECT d.*, p.nome AS nome_parco FROM distributori d " +
                     "JOIN parchi p ON d.id_parco = p.id WHERE d.id_parco=? ORDER BY d.id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idParco);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore findByParco: " + e.getMessage());
        }
        return list;
    }

    public Distributore create(Distributore d) {
        String sql = "INSERT INTO distributori (id_parco) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdParco());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore create: " + e.getMessage());
        }
        return d;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM distributori WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore delete: " + e.getMessage());
        }
        return false;
    }

    public void updateStatoGuasta(int id, boolean guasta) {
        String sql = "UPDATE distributori SET guasta=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, guasta);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore updateStatoGuasta: " + e.getMessage());
        }
    }

    public void updateUltimoContatto(int id) {
        String sql = "UPDATE distributori SET online=TRUE, ultimo_contatto=NOW() WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DistributoreDao] Errore updateUltimoContatto: " + e.getMessage());
        }
    }

    private Distributore mapRow(ResultSet rs) throws SQLException {
        Distributore d = new Distributore();
        d.setId(rs.getInt("id"));
        d.setIdParco(rs.getInt("id_parco"));
        d.setGuasta(rs.getBoolean("guasta"));
        d.setOnline(rs.getBoolean("online"));
        Timestamp ts = rs.getTimestamp("ultimo_contatto");
        if (ts != null) d.setUltimoContatto(ts.toLocalDateTime());
        try { d.setNomeParco(rs.getString("nome_parco")); } catch (SQLException ignored) {}
        return d;
    }
}
