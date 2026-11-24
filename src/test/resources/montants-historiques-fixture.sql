-- Fixture synthétique, à exécuter uniquement sur une base de test vide.
\set ON_ERROR_STOP on
CREATE TABLE voyageurs (id bigint PRIMARY KEY, solde double precision NOT NULL);
CREATE TABLE vehicules (id bigint PRIMARY KEY, prix double precision);
CREATE TABLE trajets (
    id bigint PRIMARY KEY,
    vehicule_id bigint REFERENCES vehicules(id),
    prix_total double precision NOT NULL,
    prix_par_passager double precision NOT NULL
);
CREATE TABLE paiements (id bigint PRIMARY KEY, trajet_id bigint REFERENCES trajets(id),
    montant double precision NOT NULL);
INSERT INTO voyageurs VALUES (1, 123.45), (2, 0.001);
INSERT INTO vehicules VALUES (1, 89.99), (2, NULL);
INSERT INTO trajets VALUES (1, 1, 100, 25);
INSERT INTO paiements VALUES (1, 1, 25);
