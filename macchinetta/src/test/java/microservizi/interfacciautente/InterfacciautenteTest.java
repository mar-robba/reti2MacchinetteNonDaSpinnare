package microservizi.interfacciautente;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterfacciautenteTest {

    private Interfacciautente interfaccia;
    private final int testId = 996;
    private final String dbPath = "macchinetta_" + testId + "_test.db";

    @BeforeEach
    void setUp() {
        interfaccia = new Interfacciautente(testId, "dummy_password", "test");
    }

    @AfterEach
    void tearDown() {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testInizializzazioneParametri() {
        assertEquals("macchinetta/" + testId + "/", interfaccia.getTopicRadix());
        assertNotNull(interfaccia.getListaSubscribers());
        assertEquals(0, interfaccia.getListaSubscribers().size(), "Al momento non dovrebbero esserci subscriber avviati");
    }


    @Test
    void testGetPrezzoBevandaEsistente() {
        double prezzo = interfaccia.getPrezzoBevanda(1);
        assertTrue(prezzo > 0.0, "Il prezzo della bevanda 1 dovrebbe essere positivo");
    }
    @Test
    void testSelezionaBevandaInesistente() {
        assertDoesNotThrow(() -> {
            interfaccia.selezionaBevanda(999);
        });
    }


   // ===================== TEST AVANZATI ==========================

    @Test
    void testGetBevandeFiltroDisponibile() {
        // Disabilitiamo la bevanda 1 tramite connessione diretta
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE bevande SET disponibile = 0 WHERE id = 1");
        } catch (Exception e) {
            fail(e);
        }

        List<String> bevande = interfaccia.getBevande();
        assertNotNull(bevande);

        // Verifica che la bevanda 1 non sia presente
        for (String b : bevande) {
            assertFalse(b.startsWith("1:"), "La bevanda 1 non dovrebbe comparire poichè non disponibile");
        }
    }

    @Test
    void testGetPrezzoBevandaNonDisponibile() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE bevande SET disponibile = 0 WHERE id = 1");
        } catch (Exception e) {
            fail(e);
        }
        
        double prezzo = interfaccia.getPrezzoBevanda(1);
        assertEquals(-1.0, prezzo, "Il prezzo di una bevanda non disponibile dovrebbe restituire -1");
    }

    @Test
    void testBugSceltaRispostaEccezioniNonGestite() {
        // Test fallirà perché `sceltaRisposta` lancia un'eccezione non gestita invece di
        // ignorare i messaggi scorretti elegantemente
        assertDoesNotThrow(() -> {
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/creditoInserito", "non_un_numero");
        }, "L'errore parsing del MQTT deve essere gestito e non lanciare eccezioni verso il chiamante");

        assertDoesNotThrow(() -> {
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/moneteRestituite", "ancora_non_un_numero");
        }, "Manca il try-catch per prevenire crash su moneteRestituite");

        assertDoesNotThrow(() -> {
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/resto", "null");
        }, "Manca il try-catch per prevenire crash su resto");
    }

    @Test
    void testPayloadMqttErratiMaStringa() {
        // In "/errore", il payload è una stringa qualsiasi, quindi non dovrebbe esplodere
        assertDoesNotThrow(() -> {
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/errore", "testo di errore libero");
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/bevandaErogata", "Caffè lungo");
            interfaccia.sceltaRisposta("macchinetta/" + testId + "/interfaccia/espelliMoneta", "0.01");
        });
    }

    @Test
    void testTopicSconosciuto() {
        // Topic sconosciuto dovrebbe essere ignorato silenziosamente (System.out)
        assertDoesNotThrow(() -> {
            interfaccia.sceltaRisposta("topic/sconosciuto", "123");
        });
    }
}
