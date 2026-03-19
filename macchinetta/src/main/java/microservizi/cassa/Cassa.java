package microservizi.cassa;

import microservizi.db.DBManagement;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

/**
 * Classe Cassa – Logica principale della cassa del distributore automatico.
 * Come da diagramma delle classi pagina 7.
 *
 * Gestisce: inserimento monete, controllo prezzi, calcolo resto,
 * lettura/scrittura DB locale.
 */
public class Cassa {

    private int idMacchinetta;
    private double cassaTemporanea;  // credito inserito dall'utente
    private int capacita;             // soglia cassa piena
    private DBManagement db;
    private String mode;

    // Monete ammesse (in euro)
    private static final List<Double> MONETE_AMMESSE = Arrays.asList(
            0.05, 0.10, 0.20, 0.50, 1.00, 2.00
    );

    /**
     * @param idMacchinetta id della macchinetta
     * @param mode          "dev" o "test"
     */
    public Cassa(int idMacchinetta, String mode) {
        this.idMacchinetta = idMacchinetta;
        this.mode = mode;
        this.cassaTemporanea = 0.0;
        this.db = new DBManagement(mode, idMacchinetta);

        // Leggi la capacità dal DB
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT capacita FROM cassa WHERE id = 1")) {
            if (rs.next()) {
                this.capacita = rs.getInt("capacita");
            } else {
                this.capacita = 500; // default
            }
        } catch (SQLException e) {
            this.capacita = 500;
            System.err.println("[Cassa] Errore lettura capacità: " + e.getMessage());
        }

        System.out.println("[Cassa] Inizializzata per macchinetta " + idMacchinetta
                + " (capacità: " + capacita + ", mode: " + mode + ")");
    }

    public int getIdMacchinetta() {
        return idMacchinetta;
    }

    public double getCassaTemporanea() {
        return cassaTemporanea;
    }

    public int getCapacita() {
        return capacita;
    }

    public void setCassaTemporanea(double cassaTemporanea) {
        this.cassaTemporanea = cassaTemporanea;
    }

    /**
     * Controlla se la cassa ha raggiunto la capacità massima.
     * Se true, va segnalato all'assistenza (flag cassa piena).
     */
    public boolean cassaPiena() {
        double totaleCassa = leggiJsonCassa();
        return totaleCassa >= capacita;
    }

    /**
     * Inserisce una moneta nella cassa temporanea.
     * La moneta deve essere tra quelle ammesse (STM pag.11).
     *
     * @param importo valore della moneta
     * @return true se la moneta è stata accettata, false se rifiutata
     */
    public boolean inserimentoMonete(double importo) {
        // Arrotondamento per evitare problemi di floating point
        double rounded = Math.round(importo * 100.0) / 100.0;

        if (!isMonetaValida(rounded)) {
            System.out.println("[Cassa] Moneta rifiutata: " + rounded + "€ (non ammessa)");
            return false;
        }

        cassaTemporanea += rounded;
        cassaTemporanea = Math.round(cassaTemporanea * 100.0) / 100.0;
        System.out.println("[Cassa] Moneta inserita: " + rounded + "€ | Totale temporaneo: " + cassaTemporanea + "€");
        return true;
    }

    /**
     * Verifica se una moneta è tra quelle ammesse.
     */
    private boolean isMonetaValida(double importo) {
        for (double moneta : MONETE_AMMESSE) {
            if (Math.abs(moneta - importo) < 0.001) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restituisce tutte le monete inserite dall'utente (STM pag.12).
     * Azzera la cassa temporanea.
     *
     * @return l'importo restituito
     */
    public double ridaiTutto() {
        double importo = cassaTemporanea;
        cassaTemporanea = 0.0;
        System.out.println("[Cassa] Restituite monete: " + importo + "€");
        return importo;
    }

    /**
     * Controlla se il credito temporaneo è sufficiente per la bevanda (STM pag.13).
     *
     * @param numeroBevanda numero della bevanda
     * @return true se il credito è sufficiente
     */
    public boolean controlloPrezzo(int numeroBevanda) {
        double prezzo = leggiPrezzoDB(numeroBevanda);
        if (prezzo < 0) {
            System.out.println("[Cassa] Bevanda " + numeroBevanda + " non trovata.");
            return false;
        }
        boolean sufficiente = cassaTemporanea >= prezzo;
        System.out.println("[Cassa] Controllo: credito=" + cassaTemporanea
                + "€, prezzo=" + prezzo + "€ → " + (sufficiente ? "OK" : "INSUFFICIENTE"));
        return sufficiente;
    }

    /**
     * Calcola e dà il resto dopo l'acquisto (STM pag.13).
     * Salva il pagamento nel DB in modo permanente.
     *
     * @param numeroBevanda numero della bevanda acquistata
     * @return l'importo del resto (>=0), oppure -1 se errore
     */
    public double daiResto(int numeroBevanda) {
        double prezzo = leggiPrezzoDB(numeroBevanda);
        if (prezzo < 0) {
            return -1;
        }

        double resto = cassaTemporanea - prezzo;
        resto = Math.round(resto * 100.0) / 100.0;

        // Aggiorna il totale della cassa nel DB (il prezzo della bevanda entra in cassa)
        double totaleCassa = leggiJsonCassa();
        double nuovoTotale = totaleCassa + prezzo;
        nuovoTotale = Math.round(nuovoTotale * 100.0) / 100.0;
        writeDBLocale(nuovoTotale);

        // Reset cassa temporanea
        cassaTemporanea = 0.0;

        System.out.println("[Cassa] Resto calcolato: " + resto + "€ | Nuovo totale cassa: " + nuovoTotale + "€");
        return resto;
    }

    /**
     * Legge il prezzo di una bevanda dal database.
     *
     * @param numeroBevanda id della bevanda
     * @return il prezzo, oppure -1 se non trovata
     */
    public double leggiPrezzoDB(int numeroBevanda) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT prezzo FROM bevande WHERE id = ? AND disponibile = 1")) {
            ps.setInt(1, numeroBevanda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("prezzo");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Cassa] Errore lettura prezzo: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Legge il totale corrente della cassa dal database.
     */
    public double leggiJsonCassa() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT totale FROM cassa WHERE id = 1")) {
            if (rs.next()) {
                return rs.getDouble("totale");
            }
        } catch (SQLException e) {
            System.err.println("[Cassa] Errore lettura cassa: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Aggiorna il totale della cassa nel database locale.
     *
     * @param nuovoTotale il nuovo totale della cassa
     */
    public void writeDBLocale(double nuovoTotale) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE cassa SET totale = ? WHERE id = 1")) {
            ps.setDouble(1, nuovoTotale);
            ps.executeUpdate();
            System.out.println("[Cassa] DB aggiornato: totale = " + nuovoTotale + "€");
        } catch (SQLException e) {
            System.err.println("[Cassa] Errore scrittura DB: " + e.getMessage());
        }
    }

    /**
     * Svuota la cassa (operazione del tecnico, STM pag.30).
     * Azzera il totale nel DB.
     */
    public void svuotaCassa() {
        writeDBLocale(0.0);
        System.out.println("[Cassa] Cassa svuotata dal tecnico.");
    }

    public DBManagement getDb() {
        return db;
    }
}
