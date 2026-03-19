-- Schema SQLite locale per la macchinetta
-- Ogni macchinetta ha il proprio database locale

-- Tabella bevande con prezzi
CREATE TABLE IF NOT EXISTS bevande (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    prezzo REAL NOT NULL,
    disponibile INTEGER DEFAULT 1
);

-- Tabella cassa (stato corrente)
CREATE TABLE IF NOT EXISTS cassa (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    totale REAL DEFAULT 0.0,
    capacita INTEGER DEFAULT 500
);

-- Tabella cialde (scorte)
CREATE TABLE IF NOT EXISTS cialde (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    caffe INTEGER DEFAULT 50,
    latte INTEGER DEFAULT 50,
    the INTEGER DEFAULT 50,
    cioccolata INTEGER DEFAULT 50,
    zucchero INTEGER DEFAULT 100,
    bicchieri INTEGER DEFAULT 100
);

-- Dati iniziali bevande (da pag.47 dello schema UI)
INSERT OR IGNORE INTO bevande (id, nome, prezzo, disponibile) VALUES
    (1, 'Caffe', 0.50, 1),
    (2, 'Latte caldo', 0.60, 1),
    (3, 'The', 0.50, 1),
    (4, 'Mocaccino', 0.80, 1),
    (5, 'Caffe macchiato', 0.70, 1),
    (6, 'Cioccolata fondente', 0.80, 1),
    (7, 'Cioccolata al latte', 0.80, 1);

-- Dato iniziale cassa
INSERT OR IGNORE INTO cassa (id, totale, capacita) VALUES (1, 0.0, 500);

-- Dati iniziali cialde
INSERT OR IGNORE INTO cialde (id, caffe, latte, the, cioccolata, zucchero, bicchieri)
    VALUES (1, 50, 50, 50, 50, 100, 100);
