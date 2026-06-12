DROP DATABASE banque;
CREATE DATABASE banque;
USE banque;

CREATE TABLE mouvement(
    id_mouvement INT AUTO_INCREMENT PRIMARY KEY,
    num_compte VARCHAR(100) UNIQUE,
    daty DATE,
    typany VARCHAR(20),
    montant DECIMAL(10,2)
);

CREATE TABLE cheque(
    id_cheque INT AUTO_INCREMENT PRIMARY KEY,
    num_cheque VARCHAR(100) UNIQUE,
    num_compte VARCHAR(100),
    date_validiter DATE,
    FOREIGN KEY (num_compte) REFERENCES mouvement(num_compte)
);

CREATE TABLE cheque_status(
    id_statut INT AUTO_INCREMENT PRIMARY KEY,
    statut VARCHAR(20)
);

CREATE TABLE cheque_etat(
    id_etat INT AUTO_INCREMENT PRIMARY KEY,
    id_cheque INT,
    id_statut INT,
    daty DATE,
    FOREIGN KEY (id_cheque) REFERENCES cheque(id_cheque),
    FOREIGN KEY (id_statut) REFERENCES cheque_status(id_statut)
);

INSERT INTO mouvement(num_compte, daty, typany, montant) VALUES
(1001, '2025-01-05', 'Credit', 1500),
(1002, '2025-01-10', 'Debit',  300),
(1003, '2025-01-12', 'Credit', 2000),
(1004, '2025-01-15', 'Credit', 5000),
(1005, '2025-01-18', 'Debit',  700),
(1006, '2025-01-20', 'Credit', 1200);

INSERT INTO cheque (num_cheque, num_compte, date_validiter) VALUES
(50001, 1001, '2026-02-01'),
(50002, 1002, '2026-02-10'),
(50003, 1003, '2026-02-15'),
(50004, 1004, '2026-03-01'),
(50005, 1005, '2026-03-10'),
(50006, 1006, '2026-03-15');

INSERT INTO cheque_status (statut) VALUES
('Actif'),
('Cheque vole'),
('Cheque deja encaisse');


INSERT INTO cheque_etat(id_cheque, id_statut, daty) VALUES
(1, 1, NULL),
(2, 2, NULL),
(3, 3, '2025-01-20'),
(4, 1, NULL),
(5, 3, '2025-02-01'),
(6, 1, NULL);