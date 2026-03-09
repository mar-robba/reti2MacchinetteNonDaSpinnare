package com.smartfeeder.server.model;

import java.time.LocalDateTime;

/**
 * Modello Ticket Guasto — richiesta di manutenzione.
 */
public class TicketGuasto {
    private int id;
    private String tipoGuasto;
    private int idDistributore;
    private LocalDateTime timestampRichiesta;
    private String stato; // "aperta", "in_corso", "risolta"

    // Campi extra per le risposte API
    private String nomeParco;

    public TicketGuasto() {}

    public TicketGuasto(int id, String tipoGuasto, int idDistributore,
                        LocalDateTime timestampRichiesta, String stato) {
        this.id = id;
        this.tipoGuasto = tipoGuasto;
        this.idDistributore = idDistributore;
        this.timestampRichiesta = timestampRichiesta;
        this.stato = stato;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTipoGuasto() { return tipoGuasto; }
    public void setTipoGuasto(String tipoGuasto) { this.tipoGuasto = tipoGuasto; }
    public int getIdDistributore() { return idDistributore; }
    public void setIdDistributore(int idDistributore) { this.idDistributore = idDistributore; }
    public LocalDateTime getTimestampRichiesta() { return timestampRichiesta; }
    public void setTimestampRichiesta(LocalDateTime timestampRichiesta) { this.timestampRichiesta = timestampRichiesta; }
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    public String getNomeParco() { return nomeParco; }
    public void setNomeParco(String nomeParco) { this.nomeParco = nomeParco; }
}
