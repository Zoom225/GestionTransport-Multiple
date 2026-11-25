-- Équivalent des invariants antérieurs, pour une base neuve aux mappings Transport.
-- Les scripts historiques restent inchangés et ne doivent pas être rejoués après renommage.
BEGIN;
ALTER TABLE vehicules ALTER COLUMN disponible SET DEFAULT false;
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_statut_check;
ALTER TABLE reservations ADD CONSTRAINT reservations_statut_check
  CHECK (statut IN ('EN_ATTENTE','CONFIRMEE','ANNULEE','EXPIREE'));
CREATE UNIQUE INDEX IF NOT EXISTS ux_reservations_actives
  ON reservations(trajet_id,voyageur_id) WHERE statut IN ('EN_ATTENTE','CONFIRMEE');
CREATE UNIQUE INDEX IF NOT EXISTS uq_penalite_source_evenement ON restrictions(source_evenement);
ALTER TABLE trajets DROP CONSTRAINT IF EXISTS ck_trajets_covoiturage;
ALTER TABLE trajets ADD CONSTRAINT ck_trajets_covoiturage CHECK (
  ville_depart IS NULL OR ville_arrivee IS NULL OR
  (ville_depart <> ville_arrivee AND date_fin > date_debut AND nb_passagers_actuels BETWEEN 0 AND 4)
) NOT VALID;
ALTER TABLE trajets VALIDATE CONSTRAINT ck_trajets_covoiturage;
COMMIT;
