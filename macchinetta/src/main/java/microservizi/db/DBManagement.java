package microservizi.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*; import java.util.stream.Collectors;
/**
 * Gestione del database SQLite locale della macchinetta.
 * Come da diagramma delle classi pagina 7.
 */
public class DBManagement {

    private String dbPath;

    /**
     * @param devOrTest "dev" per il database di produzione, "test" per quello di test
     * @param idMacchinetta id della macchinetta per differenziare i DB
     */
    public DBManagement(String devOrTest, int idMacchinetta) {
        if ("test".equalsIgnoreCase(devOrTest)) {
            this.dbPath = "jdbc:sqlite:macchinetta_" + idMacchinetta + "_test.db";
        } else {
            this.dbPath = "jdbc:sqlite:macchinetta_" + idMacchinetta + ".db";
        }
        initializeDB();
    }

    /**
     * Inizializza il database eseguendo lo script SQL.
     */
    private void initializeDB() {
        try (Connection conn = getConnection()) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("init_db.sql");
            if (is != null) {
                String sql = new BufferedReader(new InputStreamReader(is))
                        .lines()
                        .filter(line -> !line.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"));
                // Esegui ogni statement separatamente
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
                System.out.println("[DB] Database inizializzato: " + dbPath);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore inizializzazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restituisce una connessione al database SQLite.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbPath); //import java.sql.*;
        /*
        A cosa serve nello specifico?
Ecco come funziona il "dietro le quinte" quando scrivi DriverManager.getConnection(dbPath) nel tuo codice:
    Il Riconoscimento: Tu gli passi una stringa come jdbc:sqlite:macchinetta_1.db.
    La Selezione: DriverManager guarda l'inizio della stringa (jdbc:sqlite:). Capisce che stai parlando con SQLite e non con MySQL o Oracle.
    La Connessione: Cerca tra le librerie del tuo progetto un "Driver" che sappia gestire SQLite. Se lo trova, gli chiede di creare una connessione (Connection) e te la consegna.
        */
    }

// ma che cazzo serve?
    /**
     * Legge l'intero database locale come JsonObject.
     *
     * @param devOrTest modalità (non usato qui, il path è già impostato nel costruttore)
     * @return JsonObject con bevande, cassa, cialde
     */
    public JsonObject readDBLocalAsJson(String devOrTest) {
        JsonObject result = new JsonObject();

        try (Connection conn = getConnection()) {
            // Bevande
            JsonArray bevandeArray = new JsonArray();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM bevande")) {
                while (rs.next()) {
                    JsonObject bevanda = new JsonObject();
                    bevanda.addProperty("id", rs.getInt("id"));
                    bevanda.addProperty("nome", rs.getString("nome"));
                    bevanda.addProperty("prezzo", rs.getDouble("prezzo"));
                    bevanda.addProperty("disponibile", rs.getInt("disponibile"));
                    bevandeArray.add(bevanda);
                }
            }
            result.add("bevande", bevandeArray);

            // Cassa
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM cassa WHERE id = 1")) {
                if (rs.next()) {
                    JsonObject cassa = new JsonObject();
                    cassa.addProperty("totale", rs.getDouble("totale"));
                    cassa.addProperty("capacita", rs.getInt("capacita"));
                    result.add("cassa", cassa);
                }
            }

            // Cialde
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM cialde WHERE id = 1")) {
                if (rs.next()) {
                    JsonObject cialde = new JsonObject();
                    cialde.addProperty("caffe", rs.getInt("caffe"));
                    cialde.addProperty("latte", rs.getInt("latte"));
                    cialde.addProperty("the", rs.getInt("the"));
                    cialde.addProperty("cioccolata", rs.getInt("cioccolata"));
                    cialde.addProperty("zucchero", rs.getInt("zucchero"));
                    cialde.addProperty("bicchieri", rs.getInt("bicchieri"));
                    result.add("cialde", cialde);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB] Errore lettura JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Legge l'intero database locale come stringa JSON.
     */
    public String readDBLocalAsString() {
        return readDBLocalAsJson("dev").toString();
    }
}
