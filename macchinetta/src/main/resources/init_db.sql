-- Schema SQLite locale per la macchinetta
-- Ogni macchinetta ha il proprio database locale
-- AVVISO : non mettere commenti al fondo delle righe per non rischiare di intaccare il parsing delle dell'SQL

-- Tabella bevande con prezzi
CREATE TABLE IF NOT EXISTS bevande (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    prezzo REAL NOT NULL,
    -- flag di disponibilità
    disponibile INTEGER DEFAULT 1
);

-- Tabella cassa (stato corrente)
-- si modificherà sempre la stessa riga
CREATE TABLE IF NOT EXISTS cassa (
--Il CHECK (id = 1) impedisce a chiunque (per errore o bug nel codice) di inserire una seconda riga
    id INTEGER PRIMARY KEY CHECK (id = 1),
    totale REAL DEFAULT 0.0,
    -- 20 (poco) per il testing
    capacita INTEGER DEFAULT 20
);

-- anche qui si modificherà soltanto una riga
-- Tabella cialde (scorte) diminuisci per il testing suglla manutenzione
CREATE TABLE IF NOT EXISTS cialde (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    caffe INTEGER DEFAULT 5,
    latte INTEGER DEFAULT 5,
    the INTEGER DEFAULT 5,
    cioccolata INTEGER DEFAULT 5,
    zucchero INTEGER DEFAULT 10,
    bicchieri INTEGER DEFAULT 10
);
-- quindi gli id sono quasi inutili tanto non verrano aggiunte altre rige alla tabella



-- ============ Dati iniziali bevande (da pag.47 dello schema UI) =========

INSERT OR IGNORE INTO bevande (id, nome, prezzo, disponibile) VALUES
    (1, 'Caffe', 0.50, 1),
    (2, 'Latte caldo', 0.60, 1),
    (3, 'The', 0.50, 1),
    (4, 'Mocaccino', 0.80, 1),
    (5, 'Caffe macchiato', 0.70, 1),
    (6, 'Cioccolata fondente', 0.80, 1),
    (7, 'Cioccolata al latte', 0.80, 1);

-- Dato iniziale cassa
INSERT OR IGNORE INTO cassa (id, totale, capacita) VALUES (1, 0.0, 20);

-- Dati iniziali cialde
INSERT OR IGNORE INTO cialde (id, caffe, latte, the, cioccolata, zucchero, bicchieri)
    VALUES (1, 5, 5, 5, 5, 10, 10);
