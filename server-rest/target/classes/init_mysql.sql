-- Schema MySQL per il sistema gestionale PISSIR
-- Eseguendo questo script si crea il database con le tabelle necessarie

CREATE DATABASE IF NOT EXISTS pissir_db;
USE pissir_db;

-- Database per Keycloak (creato separatamente)
CREATE DATABASE IF NOT EXISTS keycloak_db;

-- Tabella scuole (istituti)
CREATE TABLE IF NOT EXISTS scuole (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    indirizzo VARCHAR(255),
    citta VARCHAR(100),
    provincia VARCHAR(50),
    cap VARCHAR(10),
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella macchinette
CREATE TABLE IF NOT EXISTS macchinette (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_scuola INT NOT NULL,
    nome VARCHAR(100),
    stato ENUM('ATTIVA', 'GUASTO', 'MANUTENZIONE', 'DISATTIVATA') DEFAULT 'ATTIVA',
    cassa_totale DOUBLE DEFAULT 0.0,
    cassa_capacita INT DEFAULT 500,
    flag_cassa_piena BOOLEAN DEFAULT FALSE,
    flag_cialde_esaurimento BOOLEAN DEFAULT FALSE,
    flag_zucchero_esaurimento BOOLEAN DEFAULT FALSE,
    flag_bicchieri_esaurimento BOOLEAN DEFAULT FALSE,
    flag_guasto_generico BOOLEAN DEFAULT FALSE,
    ultimo_aggiornamento TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_scuola) REFERENCES scuole(id) ON DELETE CASCADE
);

-- Tabella richieste tecnico
CREATE TABLE IF NOT EXISTS richieste_tecnico (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_macchinetta INT NOT NULL,
    tipo_guasto VARCHAR(255) NOT NULL,
    descrizione TEXT,
    stato ENUM('APERTA', 'IN_CORSO', 'COMPLETATA') DEFAULT 'APERTA',
    data_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_chiusura TIMESTAMP NULL,
    FOREIGN KEY (id_macchinetta) REFERENCES macchinette(id) ON DELETE CASCADE
);

-- Dati di esempio
INSERT INTO scuole (nome, indirizzo, citta, provincia, cap) VALUES
    ('Liceo Scientifico Calini', 'Via Monte Suello 2', 'Brescia', 'BS', '25128'),
    ('IIS Tartaglia-Olivieri', 'Via G. Oberdan 12e', 'Brescia', 'BS', '25128');

INSERT INTO macchinette (id_scuola, nome, stato) VALUES
    (1, 'Macchinetta Piano Terra', 'ATTIVA'),
    (1, 'Macchinetta Primo Piano', 'ATTIVA'),
    (2, 'Macchinetta Ingresso', 'ATTIVA');
