DROP DATABASE IF EXISTS JavaWebServer;
CREATE DATABASE JavaWebServer;
USE JavaWebServer;

create table nominativo
(
idnome int primary key auto_increment not null,
cognome varchar(200) not null,
nome varchar(200) not null
);

insert into nominativo(cognome,nome) values 
("Chiarotti","Leonardo"),
("Majid","Alessio"),
("Francesco","Lazzarelli"),
("Donald","Trump");

