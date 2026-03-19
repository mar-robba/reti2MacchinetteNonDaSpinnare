package microservizi.erogatore;

import microservizi.db.DBManagement;

import java.sql.*;
import java.util.LinkedHashMap;

/**
 * Classe Erogatore – Gestisce l'erogazione delle bevande.
 * Come da diagramma delle classi pagina 8.
 *
 * Gestisce: cialde, zucchero, bicchieri, erogazione bevande,
 * aggiornamento scorte nel DB locale.
 */
public class Erogatore {

    private int idMacchinetta;
    private int numBevanda;
    private int zucchero;
    private LinkedHashMap<String, Integer> cialde;
    private String mode;
    private DBManagement db;

    // Soglia minima per segnalare esaurimento
    private static final int SOGLIA_CIALDE = 5;
    private static final int SOGLIA_ZUCCHERO = 10;
    private static final int SOGLIA_BICCHIERI = 5;

    /**
     * @param idMacchinetta id della macchinetta
     * @param mode          "dev" o "test"
     */
    public Erogatore(int idMacchinetta, String mode) {
        this.idMacchinetta = idMacchinetta;
        this.mode = mode;
        this.numBevanda = 0;
        this.zucchero = 0;
        this.db = new DBManagement(mode, idMacchinetta);
        this.cialde = new LinkedHashMap<>();

        leggiJsonCialde();
        System.out.println("[Erogatore] Inizializzato per macchinetta " + idMacchinetta + " | Cialde: " + cialde);
    }

    public int getIdMacchinetta() { return idMacchinetta; }
    public int getNumBevanda() { return numBevanda; }
    public void setNumBevanda(int num) { this.numBevanda = num; }
    public int getZucchero() { return zucchero; }
    public void setZucchero(int zucchero) { this.zucchero = zucchero; }
    public LinkedHashMap<String, Integer> getCialde() { return cialde; }

    /**
     * Controlla lo stato delle cialde e restituisce eventuali alert.
     * (Pagina 8: controlloCialde())
     *
     * @return stringa di stato ("OK", "CIALDE_ESAURIMENTO", "ZUCCHERO_ESAURIMENTO", ecc.)
     */
    public String controlloCialde() {
        StringBuilder alerts = new StringBuilder();

        for (var entry : cialde.entrySet()) {
            if (entry.getValue() <= SOGLIA_CIALDE) {
                if (alerts.length() > 0) alerts.append(",");
                alerts.append("CIALDE_").append(entry.getKey().toUpperCase()).append("_ESAURIMENTO");
            }
        }

        int zuccheroDb = cialde.getOrDefault("zucchero", 0);
        if (zuccheroDb <= SOGLIA_ZUCCHERO) {
            if (alerts.length() > 0) alerts.append(",");
            alerts.append("ZUCCHERO_ESAURIMENTO");
        }

        int bicchieri = cialde.getOrDefault("bicchieri", 0);
        if (bicchieri <= SOGLIA_BICCHIERI) {
            if (alerts.length() > 0) alerts.append(",");
            alerts.append("BICCHIERI_ESAURIMENTO");
        }

        return alerts.length() > 0 ? alerts.toString() : "OK";
    }

    /**
     * Aggiorna le cialde dopo l'erogazione di una bevanda.
     * Decrementa la cialda corrispondente, lo zucchero e i bicchieri.
     */
    public void aggiornaCialde() {
        // Mappa bevanda → tipo cialda
        String tipoCialda = switch (numBevanda) {
            case 1 -> "caffe";      // Caffè
            case 2 -> "latte";      // Latte caldo
            case 3 -> "the";        // Thè
            case 4 -> "cioccolata"; // Mocaccino (cioccolata + caffe)
            case 5 -> "caffe";      // Caffè macchiato
            case 6 -> "cioccolata"; // Cioccolata fondente
            case 7 -> "cioccolata"; // Cioccolata al latte
            default -> null;
        };

        if (tipoCialda != null) {
            cialde.merge(tipoCialda, -1, Integer::sum);
        }

        // Il Mocaccino usa anche caffè
        if (numBevanda == 4) {
            cialde.merge("caffe", -1, Integer::sum);
        }

        // Caffè macchiato usa anche latte
        if (numBevanda == 5) {
            cialde.merge("latte", -1, Integer::sum);
        }

        // Cioccolata al latte usa anche latte
        if (numBevanda == 7) {
            cialde.merge("latte", -1, Integer::sum);
        }

        // Decrementa zucchero (se selezionato) e bicchiere
        cialde.merge("zucchero", -zucchero, Integer::sum);
        cialde.merge("bicchieri", -1, Integer::sum);

        // Salva nel DB
        writeDBLocale(
                cialde.getOrDefault("caffe", 0),
                cialde.getOrDefault("latte", 0),
                cialde.getOrDefault("the", 0),
                cialde.getOrDefault("cioccolata", 0),
                cialde.getOrDefault("zucchero", 0),
                cialde.getOrDefault("bicchieri", 0)
        );

        System.out.println("[Erogatore] Cialde aggiornate dopo bevanda " + numBevanda + ": " + cialde);
    }

    /**
     * Legge il numero di cialde disponibili per una bevanda specifica.
     *
     * @param numeroBevanda id della bevanda
     * @return array [cialdeBevanda, zuccheroDisponibile]
     */
    public int[] leggiCialdeDB(int numeroBevanda) {
        leggiJsonCialde();

        String tipoCialda = switch (numeroBevanda) {
            case 1, 5 -> "caffe";
            case 2 -> "latte";
            case 3 -> "the";
            case 4, 6, 7 -> "cioccolata";
            default -> null;
        };

        int cialdeBevanda = tipoCialda != null ? cialde.getOrDefault(tipoCialda, 0) : 0;
        int zuccheroDisp = cialde.getOrDefault("zucchero", 0);

        return new int[]{cialdeBevanda, zuccheroDisp};
    }

    /**
     * Legge lo stato delle cialde dal database SQLite.
     */
    public void leggiJsonCialde() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cialde WHERE id = 1")) {
            if (rs.next()) {
                cialde.clear();
                cialde.put("caffe", rs.getInt("caffe"));
                cialde.put("latte", rs.getInt("latte"));
                cialde.put("the", rs.getInt("the"));
                cialde.put("cioccolata", rs.getInt("cioccolata"));
                cialde.put("zucchero", rs.getInt("zucchero"));
                cialde.put("bicchieri", rs.getInt("bicchieri"));
            }
        } catch (SQLException e) {
            System.err.println("[Erogatore] Errore lettura cialde: " + e.getMessage());
        }
    }

    /**
     * Scrive lo stato delle cialde nel database locale.
     */
    public void writeDBLocale(int caffe, int latte, int the, int cioccolata, int zucchero, int bicchieri) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE cialde SET caffe=?, latte=?, the=?, cioccolata=?, zucchero=?, bicchieri=? WHERE id=1")) {
            ps.setInt(1, caffe);
            ps.setInt(2, latte);
            ps.setInt(3, the);
            ps.setInt(4, cioccolata);
            ps.setInt(5, zucchero);
            ps.setInt(6, bicchieri);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Erogatore] Errore scrittura DB: " + e.getMessage());
        }
    }

    /**
     * Verifica se la bevanda selezionata è disponibile (cialde e bicchieri sufficienti).
     */
    public boolean bevandaDisponibile(int numeroBevanda) {
        int[] disponibilita = leggiCialdeDB(numeroBevanda);
        int bicchieri = cialde.getOrDefault("bicchieri", 0);
        return disponibilita[0] > 0 && bicchieri > 0;
    }

    public DBManagement getDb() { return db; }
}
