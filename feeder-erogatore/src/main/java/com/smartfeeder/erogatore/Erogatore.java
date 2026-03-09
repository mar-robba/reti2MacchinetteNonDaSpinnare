package com.smartfeeder.erogatore;

import com.smartfeeder.common.DBManagement;

/**
 * Microservizio Erogatore.
 * Gestisce il portello del mangime e il sensore del serbatoio.
 * Se il mangime si esaurisce, segnala il guasto.
 */
public class Erogatore {

    private final int idDistributore;
    private int mangimeDisponibile;
    private final DBManagement db;

    public Erogatore(int idDistributore) {
        this.idDistributore = idDistributore;
        this.db = new DBManagement(idDistributore);
        this.mangimeDisponibile = db.getMangimeDisponibile();
    }

    public int getIdDistributore() { return idDistributore; }

    public int getMangimeDisponibile() { return mangimeDisponibile; }

    /**
     * Controlla se il mangime è esaurito.
     */
    public boolean mangimeEsaurito() {
        return mangimeDisponibile <= 0;
    }

    /**
     * Eroga una porzione di mangime.
     * @return true se erogazione riuscita
     */
    public boolean erogaPorzione() {
        if (mangimeEsaurito()) {
            System.out.println("[Erogatore] Mangime esaurito! Impossibile erogare.");
            return false;
        }
        mangimeDisponibile--;
        db.updateMangimeDisponibile(mangimeDisponibile);
        System.out.println("[Erogatore] Porzione erogata. Mangime rimanente: " + mangimeDisponibile);
        // Simula apertura portello
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return true;
    }

    /**
     * Ricarica il mangime (dopo intervento tecnico).
     */
    public void ricaricaMangime(int quantita) {
        mangimeDisponibile = quantita;
        db.updateMangimeDisponibile(mangimeDisponibile);
        System.out.println("[Erogatore] Mangime ricaricato: " + quantita + " porzioni.");
    }

    public String getStatoJSON() {
        return db.readDBLocalAsString();
    }

    public void close() { db.close(); }
}
