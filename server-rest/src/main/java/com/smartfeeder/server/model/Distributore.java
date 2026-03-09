package com.smartfeeder.server.model;

import java.time.LocalDateTime;

/**
 * Modello Distributore — singolo dispositivo IoT.
 */
public class Distributore {
    private int id;
    private int idParco;
    private boolean guasta;
    private boolean online;
    private LocalDateTime ultimoContatto;

    // Nome del parco (per le risposte API)
    private String nomeParco;

    public Distributore() {}

    public Distributore(int id, int idParco, boolean guasta, boolean online, LocalDateTime ultimoContatto) {
        this.id = id;
        this.idParco = idParco;
        this.guasta = guasta;
        this.online = online;
        this.ultimoContatto = ultimoContatto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdParco() { return idParco; }
    public void setIdParco(int idParco) { this.idParco = idParco; }
    public boolean isGuasta() { return guasta; }
    public void setGuasta(boolean guasta) { this.guasta = guasta; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public LocalDateTime getUltimoContatto() { return ultimoContatto; }
    public void setUltimoContatto(LocalDateTime ultimoContatto) { this.ultimoContatto = ultimoContatto; }
    public String getNomeParco() { return nomeParco; }
    public void setNomeParco(String nomeParco) { this.nomeParco = nomeParco; }
}
