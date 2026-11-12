-- Après sauvegarde et revue. Transaction atomique, aucune ligne supprimée ou réécrite.
BEGIN;
ALTER TABLE terrains ADD COLUMN IF NOT EXISTS disponible boolean NOT NULL DEFAULT false;
ALTER TABLE terrains ALTER COLUMN disponible SET DEFAULT false;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS ville_depart varchar(255);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS ville_arrivee varchar(255);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS publication_automatique_autorisee boolean NOT NULL DEFAULT false;
ALTER TABLE membres ADD COLUMN IF NOT EXISTS peut_proposer_trajet boolean NOT NULL DEFAULT false;
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM reservations WHERE statut IN ('EN_ATTENTE', 'CONFIRMEE')
    GROUP BY match_id, membre_id HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION 'Doublons actifs existants : régularisation métier requise avant migration, aucune suppression automatique.';
  END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_reservations_actives
  ON reservations (match_id, membre_id) WHERE statut IN ('EN_ATTENTE', 'CONFIRMEE');
CREATE INDEX IF NOT EXISTS idx_trajets_recherche
  ON matches (type_match, statut, ville_depart, ville_arrivee, date_debut);
CREATE INDEX IF NOT EXISTS idx_trajets_chauffeur_creneau
  ON matches (organisateur_id, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_trajets_vehicule_creneau
  ON matches (terrain_id, date_debut, date_fin);
ALTER TABLE matches DROP CONSTRAINT IF EXISTS ck_trajets_covoiturage;
ALTER TABLE matches ADD CONSTRAINT ck_trajets_covoiturage CHECK (
  ville_depart IS NULL OR ville_arrivee IS NULL OR
  (ville_depart <> ville_arrivee AND date_fin > date_debut AND nb_joueurs_actuels BETWEEN 0 AND 4)
) NOT VALID;
ALTER TABLE matches VALIDATE CONSTRAINT ck_trajets_covoiturage;
COMMIT;
