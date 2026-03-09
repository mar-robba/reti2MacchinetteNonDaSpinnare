-- Smart Feeder Database Schema
-- Database: SmartFeederDB

CREATE DATABASE IF NOT EXISTS SmartFeederDB;
USE SmartFeederDB;

-- Tabella Parchi (i luoghi di installazione)
CREATE TABLE IF NOT EXISTS parchi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    indirizzo VARCHAR(255) NOT NULL,
    citta VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabella Distributori (singoli dispositivi IoT)
CREATE TABLE IF NOT EXISTS distributori (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_parco INT NOT NULL,
    guasta BOOLEAN NOT NULL DEFAULT FALSE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_contatto DATETIME NULL,
    FOREIGN KEY (id_parco) REFERENCES parchi(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabella Ticket Guasti (richieste di manutenzione)
CREATE TABLE IF NOT EXISTS ticket_guasti (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_guasto VARCHAR(255) NOT NULL,
    id_distributore INT NOT NULL,
    timestamp_richiesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stato ENUM('aperta', 'in_corso', 'risolta') NOT NULL DEFAULT 'aperta',
    FOREIGN KEY (id_distributore) REFERENCES distributori(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dati iniziali - Parchi
INSERT INTO parchi (nome, indirizzo, citta) VALUES
('Parco Nord', 'Viale delle Querce 10', 'Torino'),
('Parco Sud', 'Via dei Platani 25', 'Torino'),
('Parco Lago', 'Lungolago Europa 5', 'Torino');

-- Dati iniziali - Distributori
INSERT INTO distributori (id, id_parco) VALUES
(1, 1),
(2, 1),
(3, 2);

-- Dati iniziali - Ticket di esempio
INSERT INTO ticket_guasti (tipo_guasto, id_distributore, stato) VALUES
('Mangime esaurito', 1, 'aperta'),
('Cassa piena', 2, 'in_corso');
