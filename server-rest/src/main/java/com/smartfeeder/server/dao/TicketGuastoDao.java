package com.smartfeeder.server.dao;

import com.smartfeeder.server.config.DatabaseManager;
import com.smartfeeder.server.model.TicketGuasto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per accesso alla tabella ticket_guasti.
 */
public class TicketGuastoDao {

    public TicketGuasto findById(int id) {
        String sql = "SELECT * FROM ticket_guasti WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore findById: " + e.getMessage());
        }
        return null;
    }

    public List<TicketGuasto> findAll() {
        List<TicketGuasto> list = new ArrayList<>();
        String sql = "SELECT tg.*, p.nome AS nome_parco FROM ticket_guasti tg " +
                     "JOIN distributori d ON tg.id_distributore = d.id " +
                     "JOIN parchi p ON d.id_parco = p.id " +
                     "ORDER BY tg.timestamp_richiesta DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore findAll: " + e.getMessage());
        }
        return list;
    }

    public List<TicketGuasto> findByStato(String stato) {
        List<TicketGuasto> list = new ArrayList<>();
        String sql = "SELECT tg.*, p.nome AS nome_parco FROM ticket_guasti tg " +
                     "JOIN distributori d ON tg.id_distributore = d.id " +
                     "JOIN parchi p ON d.id_parco = p.id " +
                     "WHERE tg.stato=? ORDER BY tg.timestamp_richiesta DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stato);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore findByStato: " + e.getMessage());
        }
        return list;
    }

    public List<TicketGuasto> findByDistributore(int idDistributore) {
        List<TicketGuasto> list = new ArrayList<>();
        String sql = "SELECT tg.*, p.nome AS nome_parco FROM ticket_guasti tg " +
                     "JOIN distributori d ON tg.id_distributore = d.id " +
                     "JOIN parchi p ON d.id_parco = p.id " +
                     "WHERE tg.id_distributore=? ORDER BY tg.timestamp_richiesta DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDistributore);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore findByDistributore: " + e.getMessage());
        }
        return list;
    }

    public TicketGuasto create(TicketGuasto ticket) {
        String sql = "INSERT INTO ticket_guasti (tipo_guasto, id_distributore, stato) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ticket.getTipoGuasto());
            ps.setInt(2, ticket.getIdDistributore());
            ps.setString(3, ticket.getStato() != null ? ticket.getStato() : "aperta");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ticket.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore create: " + e.getMessage());
        }
        return ticket;
    }

    public boolean risolvi(int ticketId) {
        String sql = "UPDATE ticket_guasti SET stato='risolta' WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TicketGuastoDao] Errore risolvi: " + e.getMessage());
        }
        return false;
    }

    private TicketGuasto mapRow(ResultSet rs) throws SQLException {
        TicketGuasto t = new TicketGuasto();
        t.setId(rs.getInt("id"));
        t.setTipoGuasto(rs.getString("tipo_guasto"));
        t.setIdDistributore(rs.getInt("id_distributore"));
        Timestamp ts = rs.getTimestamp("timestamp_richiesta");
        if (ts != null) t.setTimestampRichiesta(ts.toLocalDateTime());
        t.setStato(rs.getString("stato"));
        try { t.setNomeParco(rs.getString("nome_parco")); } catch (SQLException ignored) {}
        return t;
    }
}
