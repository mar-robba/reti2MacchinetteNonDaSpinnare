package microservizi.db;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DBManagementTest {

    private DBManagement db;
    private final int testId = 999;
    private final String dbPath = "macchinetta_" + testId + "_test.db";

    @BeforeEach
    void setUp() {
        db = new DBManagement("test", testId);
    }

    @AfterEach
    void tearDown() {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testConnectionIsNotNull() {
        try (Connection conn = db.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        } catch (SQLException e) {
            fail("Errore: " + e.getMessage());
        }
    }

    @Test
    void testReadDBLocalAsString() {
        String jsonString = db.readDBLocalAsString();
        assertNotNull(jsonString);
        assertTrue(jsonString.contains("bevande"));
        assertTrue(jsonString.contains("cassa"));
        assertTrue(jsonString.contains("cialde"));
    }


    // =========================== TEST AVANZATI ==============================

    @Test
    void testDBInitializationAndStructure() {
        JsonObject jsonDB = db.readDBLocalAsJson("test");

        assertTrue(jsonDB.has("bevande"));
        assertTrue(jsonDB.getAsJsonArray("bevande").size() > 0);

        assertTrue(jsonDB.has("cassa"));
        assertEquals(0.0, jsonDB.getAsJsonObject("cassa").get("totale").getAsDouble());
        assertTrue(jsonDB.getAsJsonObject("cassa").get("capacita").getAsInt() > 0);

        assertTrue(jsonDB.has("cialde"));
        assertTrue(jsonDB.getAsJsonObject("cialde").get("caffe").getAsInt() > 0);
    }

    @Test
    void testLetturaSimultaneaNonBloccante() {
        // Verifica che la lettura del JSON avvenga correttamente anche con più iterazioni rapide
        for (int i = 0; i < 10; i++) {
            JsonObject jsonDB = db.readDBLocalAsJson("test");
            assertNotNull(jsonDB);
        }
    }

    @Test
    void testConsistenzaCassa() {
        // Modifica la cassa tramite SQL brutale e verifica se il JSON si adatta
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE cassa SET totale = 50.5 WHERE id = 1");
        } catch (SQLException e) {
            fail();
        }

        JsonObject jsonDB = db.readDBLocalAsJson("test");
        assertEquals(50.5, jsonDB.getAsJsonObject("cassa").get("totale").getAsDouble());
    }
}
