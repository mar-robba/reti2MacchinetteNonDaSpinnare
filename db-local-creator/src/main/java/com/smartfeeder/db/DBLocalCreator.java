package com.smartfeeder.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/*
nota : le citazioni non sono esatte
# Database Locale delle Macchinette

Nel sistema di gestione delle macchinette per bevande, ogni singola unità dispone di un proprio sistema di persistenza locale per gestire lo stato interno e i dati operativi.

## Tecnologie e Componenti
* [cite_start]**Gestione Dati:** La persistenza locale è affidata alla gestione di file in formato **.json** per ogni singola macchinetta[cite: 266, 280].
* [cite_start]**Creazione:** Esiste un modulo specifico denominato `DBLocalCreator` che si occupa della generazione iniziale del database locale per la macchinetta[cite: 361].

## Responsabilità del Database Locale
[cite_start]Il database locale (DBMS locale(json)) deve monitorare costantemente lo stato delle risorse interne della macchinetta[cite: 299, 305]:

* [cite_start]**Tracciamento Cialde:** Deve tenere traccia del numero di cialde disponibili per ogni tipologia di bevanda (caffè, latte, cioccolato, tè) [cite: 135, 157-161].
* [cite_start]**Gestione Zucchero e Bicchieri:** Monitora la quantità di zucchero e il numero di bicchierini rimanenti[cite: 135, 137].
* **Contabilità Cassa:**
    * [cite_start]Gestisce la **cassa temporanea**, che trattiene il denaro inserito fino all'erogazione[cite: 138].
    * [cite_start]Gestisce la **cassa definitiva**, che accumula gli incassi totali degli acquisti effettuati[cite: 142].
* [cite_start]**Identificazione:** Memorizza l'**ID univoco** della macchinetta per permetterne la rintracciabilità da parte del server centrale[cite: 148].

## Interazione e Sincronizzazione
Il database locale interagisce con i microservizi interni e il server remoto:
* [cite_start]**Segnalazione Guasti/Esaurimento:** Quando i dati nel DB indicano che le cialde sono finite o la cassa è piena, viene attivata la comunicazione verso il tecnico tramite il microservizio di Assistenza[cite: 144, 317].
* [cite_start]**Sincronizzazione:** Il sistema prevede l'invio di messaggi di sincronizzazione JSON (es. `sync json cassa-server` o `sync json erogatore-server`) per mantenere allineato lo stato locale con il Server REST gestionale[cite: 48, 49].

 */

/**
 * Crea il database SQLite locale per un distributore.
 * Da eseguire prima di avviare gli altri microservizi.
 *
 * Utilizzo: java -jar db-local-creator.jar [idDistributore]
 */
public class DBLocalCreator {

    public static void main(String[] args) {
        int idDistributore = 1;
        if (args.length > 0) {
            try {
                idDistributore = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Argomento non valido: " + args[0] + ". Uso idDistributore=1");
            }
        }

        String dbPath = "distributore_" + idDistributore + ".db";
        System.out.println("[DBLocalCreator] Creazione DB locale: " + dbPath);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            createTables(conn);
            insertDefaultData(conn);
            System.out.println("[DBLocalCreator] DB creato con successo per distributore " + idDistributore);
        } catch (SQLException e) {
            System.err.println("[DBLocalCreator] Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Stato fisico del distributore
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS stato_distributore (
                            id INTEGER PRIMARY KEY DEFAULT 1,
                            monete_contate INTEGER DEFAULT 0,
                            capacita_cassa INTEGER DEFAULT 50,
                            mangime_disponibile INTEGER DEFAULT 100,
                            guasta INTEGER DEFAULT 0
                        )
                    """);

            // Log transazioni
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS transazioni (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            tipo TEXT,
                            timestamp TEXT DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            System.out.println("[DBLocalCreator] Tabelle create.");
        }
    }

    private static void insertDefaultData(Connection conn) throws SQLException {
        // Inserisci stato distributore se non esiste
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO stato_distributore (id) VALUES (1)")) {
            ps.executeUpdate();
        }
        System.out.println("[DBLocalCreator] Dati di default inseriti.");
    }
}
