package microservizi.cassa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CassaTest {

    private Cassa cassa;
    private final int testId = 998;
    private final String dbPath = "macchinetta_" + testId + "_test.db";

    @BeforeEach
    void setUp() {
        cassa = new Cassa(testId, "test");
        cassa.svuotaCassa();
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
        assertEquals(testId, cassa.getIdMacchinetta());
        assertEquals(0.0, cassa.getCassaTemporanea());
        assertTrue(cassa.getCapacita() > 0);
    }

    @Test
    void testRidaiTutto() {
        cassa.inserimentoMonete(1.00);
        cassa.inserimentoMonete(0.50);

        double restituito = cassa.ridaiTutto();

        assertEquals(1.50, restituito);
        assertEquals(0.0, cassa.getCassaTemporanea(), "La cassa temporanea deve essere svuotata");
        assertEquals(0.0, cassa.leggiJsonCassa(), 0.001, "Il DB non deve essere toccato dal ridaiTutto");
    }

    @Test
    void testCassaPiena() {
        assertFalse(cassa.cassaPiena(), "La cassa non dovrebbe essere piena all'inizio");
        cassa.writeDBLocale(cassa.getCapacita() + 10.0);
        assertTrue(cassa.cassaPiena(), "La cassa dovrebbe risultare piena");
    }



   // ========================== TEST AVANZATI =============================================

    @Test
    void testInserimentoMistoValideInvalide() {
        assertFalse(cassa.inserimentoMonete(0.01));
        assertTrue(cassa.inserimentoMonete(1.00));
        assertFalse(cassa.inserimentoMonete(3.00));
        assertTrue(cassa.inserimentoMonete(0.50));
        assertTrue(cassa.inserimentoMonete(0.05));
        
        assertEquals(1.55, cassa.getCassaTemporanea(), 0.001);
    }

    @Test
    void testArrotondamentoInserimento() {
        // Inserimenti ripetuti per testare problemi di virgola mobile
        for(int i=0; i<10; i++) {
            assertTrue(cassa.inserimentoMonete(0.10));
        }
        assertEquals(1.00, cassa.getCassaTemporanea(), 0.001);
    }

    @Test
    void testAcquistoConRestozero() {
        double prezzo = cassa.leggiPrezzoDB(1);
        if (prezzo > 0) { // Test condizionato all'esistenza del DB corretto
            cassa.setCassaTemporanea(prezzo);
            
            assertTrue(cassa.controlloPrezzo(1));
            
            double resto = cassa.daiResto(1);
            assertEquals(0.0, resto, 0.001);
            assertEquals(0.0, cassa.getCassaTemporanea());
            
            assertEquals(prezzo, cassa.leggiJsonCassa(), 0.001);
        }
    }

    @Test
    void testTentativoAcquistoBevandaInesistente() {
        cassa.inserimentoMonete(2.00);
        
        assertFalse(cassa.controlloPrezzo(9999), "Non deve poter comprare bevanda inesistente");
        
        double restoPrimaErrore = cassa.daiResto(9999);
        assertEquals(-1.0, restoPrimaErrore, "Deve restituire -1 per errore bevanda");
        
        // Il credito deve rimanere intatto
        assertEquals(2.00, cassa.getCassaTemporanea(), 0.001);
        assertEquals(0.0, cassa.leggiJsonCassa(), 0.001);
    }

    @Test
    void testAcquistiMultipliEdAzzeramento() {
        double prezzo = cassa.leggiPrezzoDB(1);
        if (prezzo > 0) {
            cassa.setCassaTemporanea(5.00);
            
            double resto = cassa.daiResto(1);
            double expectedResto = 5.00 - prezzo;
            assertEquals(expectedResto, resto, 0.001);
            assertEquals(prezzo, cassa.leggiJsonCassa(), 0.001);
            
            // verifica azzeramento credito
            assertFalse(cassa.controlloPrezzo(1));
            assertEquals(0.0, cassa.getCassaTemporanea(), 0.001);
        }
    }

    @Test
    void testTentativoAcquistoSenzaCreditoSufficienteChiamaDaiResto() {
        double prezzo = cassa.leggiPrezzoDB(1);
        if (prezzo > 0) {
            cassa.setCassaTemporanea(0.0);
            double resto = cassa.daiResto(1);
            
            // Il comportamento corretto deve restituire che NON c'è resto (es: 0 o errore, non negativo)
            assertTrue(resto >= 0 || resto == -1, "Il resto non può essere negativo, la transazione non dovrebbe avvenire");
            assertEquals(0.0, cassa.leggiJsonCassa(), 0.001, "I soldi non devono essere scalati sulla cassa sqlite se non c'è credito");
        }
    }

}
