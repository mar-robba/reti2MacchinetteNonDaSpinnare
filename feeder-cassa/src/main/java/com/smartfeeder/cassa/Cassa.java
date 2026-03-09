package com.smartfeeder.cassa;

import com.smartfeeder.common.DBManagement;

/**
 * Microservizio Cassa.
 * Gestisce l'inserimento della moneta (1€ fisso) e il conteggio.
 * Se il cassetto monete raggiunge la capienza massima, va in blocco.
 *
 * Logica semplificata: non c'è resto, non c'è scelta prodotto.
 * 1 moneta = 1 erogazione.
 */
public class Cassa {

    private final int idDistributore;
    private int moneteContate;
    private final int capacitaCassa;
    private final DBManagement db;

    public Cassa(int idDistributore) {
        this.idDistributore = idDistributore;
        this.db = new DBManagement(idDistributore);
        this.moneteContate = db.getMoneteContate();
        this.capacitaCassa = db.getCapacitaCassa();
    }

    public int getIdDistributore() { return idDistributore; }

    public int getMoneteContate() { return moneteContate; }

    public int getCapacitaCassa() { return capacitaCassa; }

    /**
     * Registra l'inserimento della moneta (1€).
     * @return true se la moneta è stata accettata
     */
    public boolean inserimentoMoneta() {
        if (cassaPiena()) {
            System.out.println("[Cassa] Cassa piena! Moneta rifiutata.");
            return false;
        }
        moneteContate++;
        db.updateMoneteContate(moneteContate);
        System.out.println("[Cassa] Moneta inserita. Totale monete: " + moneteContate);
        return true;
    }

    /**
     * Verifica se la cassa è piena.
     */
    public boolean cassaPiena() {
        return moneteContate >= capacitaCassa;
    }

    /**
     * Svuota la cassa (dopo intervento tecnico).
     */
    public void svuotaCassa() {
        moneteContate = 0;
        db.updateMoneteContate(0);
        System.out.println("[Cassa] Cassa svuotata.");
    }

    public void close() {
        db.close();
    }
}
