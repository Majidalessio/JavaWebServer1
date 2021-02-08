DROP DATABASE IF EXISTS JavaWebServer;
CREATE DATABASE JavaWebServer;
USE JavaWebServer;

CREATE TABLE nominativo (
idnome INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
cognome VARCHAR(200) NOT NULL,
nome VARCHAR(200) NOT NULL
);

INSERT INTO nominativo(cognome,nome) VALUES
("Chiarotti","Leonardo"),
("Majid","Alessio"),
("Lizzio","Francesco"),
("Biden","Joe");

