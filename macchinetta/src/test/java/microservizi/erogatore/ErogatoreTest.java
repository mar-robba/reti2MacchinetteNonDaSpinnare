package microservizi.erogatore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ErogatoreTest {

    private Erogatore erogatore;
    private final int testId = 997;
    private final String dbPath = "macchinetta_" + testId + "_test.db";

    @BeforeEach
    void setUp() {
        erogatore = new Erogatore(testId, "test");
        erogatore.writeDBLocale(100, 100, 100, 100, 100, 100);
        erogatore.leggiJsonCialde(); 
    }

    @AfterEach
    void tearDown() {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testInizializzazione() {
        assertEquals(testId, erogatore.getIdMacchinetta());
        assertNotNull(erogatore.getCialde());
        assertEquals(100, erogatore.getCialde().get("caffe"));
    }

    @Test
    void testAggiornaCialdeCaffeSemplice() {
        erogatore.setNumBevanda(1);
        erogatore.setZucchero(2);

        int caffeIniziale = erogatore.getCialde().get("caffe");
        int zuccheroIniziale = erogatore.getCialde().get("zucchero");
        int bicchieriIniziali = erogatore.getCialde().get("bicchieri");

        erogatore.aggiornaCialde();

        assertEquals(caffeIniziale - 1, (int) erogatore.getCialde().get("caffe"));
        assertEquals(zuccheroIniziale - 2, (int) erogatore.getCialde().get("zucchero"));
        assertEquals(bicchieriIniziali - 1, (int) erogatore.getCialde().get("bicchieri"));
    }




// ================ TEST AVANZATI ============================

    @Test
    void testBevandaSingolaIngredienteDisponibile() {
        assertTrue(erogatore.bevandaDisponibile(1), "Caffè dovrebbe essere disponibile");
    }

    @Test
    void testEsaurimentoIngredientePrincipale() {
        erogatore.writeDBLocale(0, 100, 100, 100, 100, 100);
        erogatore.leggiJsonCialde();
        assertFalse(erogatore.bevandaDisponibile(1), "Caffè non deve essere disponibile se manca la cialda di caffè");
    }

    @Test
    void testBugEsaurimentoIngredienteSecondarioMocaccino() {
        // Mocaccino (numero 4) usa cioccolata + caffè. 
        erogatore.writeDBLocale(0, 100, 100, 100, 100, 100); // 0 caffe, 100 cioccolata
        erogatore.leggiJsonCialde();
        
        // Deve aspettarsi il comportamento corretto: Mocaccino NON disponibile perchè manca il Caffè
        assertFalse(erogatore.bevandaDisponibile(4), "Il Mocaccino NON deve essere disponibile se manca il caffè!");
    }

    @Test
    void testBugEsaurimentoIngredienteSecondarioMacchiato() {
        // Caffè macchiato (numero 5) usa caffè + latte
        erogatore.writeDBLocale(100, 0, 100, 100, 100, 100); // 100 caffe, 0 latte
        erogatore.leggiJsonCialde();
        
        // Deve aspettarsi il comportamento corretto: Caffè Macchiato NON disponibile perchè manca il Latte
        assertFalse(erogatore.bevandaDisponibile(5), "Il Caffè macchiato NON deve essere disponibile se manca il latte!");
    }

    @Test
    void testBugErogazioneConZuccheroInsufficiente() {
        // L'utente chiede 5 di zucchero ma ce n'è solo 1
        erogatore.writeDBLocale(100, 100, 100, 100, 1, 100);
        erogatore.leggiJsonCialde();
        
        erogatore.setNumBevanda(1);
        erogatore.setZucchero(5);
        erogatore.aggiornaCialde();
        
        // Ci si aspetta la normalità: lo zucchero residuo NON deve MAI scendere sotto 0
        int zuccheroRimasto = erogatore.getCialde().get("zucchero");
        assertTrue(zuccheroRimasto >= 0, "Lo zucchero non deve scendere in negativo");
    }


    @Test
    void testAggiornaCialdeBevandeComposte() {
        // Mocaccino (4): cioccolata + caffe
        erogatore.setNumBevanda(4); 
        erogatore.setZucchero(0);
        
        int caffeIniziale = erogatore.getCialde().get("caffe");
        int cioccolataIniziale = erogatore.getCialde().get("cioccolata");

        erogatore.aggiornaCialde();

        assertEquals(caffeIniziale - 1, (int) erogatore.getCialde().get("caffe"));
        assertEquals(cioccolataIniziale - 1, (int) erogatore.getCialde().get("cioccolata"));
        
        // Caffè macchiato (5): caffe + latte
        erogatore.setNumBevanda(5);
        erogatore.aggiornaCialde();
        assertEquals(caffeIniziale - 2, (int) erogatore.getCialde().get("caffe"));
        assertEquals(99, (int) erogatore.getCialde().get("latte"));
    }

    @Test
    void testControlloCialdeMultipliAllarmi() {
        // Scendiamo sotto tutte le soglie (soglia cialde=5, zucchero=10, bicchieri=5)
        erogatore.writeDBLocale(5, 5, 5, 5, 10, 5);
        erogatore.leggiJsonCialde();

        String alert = erogatore.controlloCialde();
        assertTrue(alert.contains("CIALDE_CAFFE_ESAURIMENTO"));
        assertTrue(alert.contains("CIALDE_LATTE_ESAURIMENTO"));
        assertTrue(alert.contains("CIALDE_THE_ESAURIMENTO"));
        assertTrue(alert.contains("CIALDE_CIOCCOLATA_ESAURIMENTO"));
        assertTrue(alert.contains("ZUCCHERO_ESAURIMENTO"));
        assertTrue(alert.contains("BICCHIERI_ESAURIMENTO"));
    }
}
