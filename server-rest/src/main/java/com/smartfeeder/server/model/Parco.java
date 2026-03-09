package com.smartfeeder.server.model;

/**
 * Modello Parco — luogo di installazione dei distributori.
 */
public class Parco {
    private int id;
    private String nome;
    private String indirizzo;
    private String citta;

    public Parco() {}

    public Parco(int id, String nome, String indirizzo, String citta) {
        this.id = id;
        this.nome = nome;
        this.indirizzo = indirizzo;
        this.citta = citta;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public String getCitta() { return citta; }
    public void setCitta(String citta) { this.citta = citta; }
}
