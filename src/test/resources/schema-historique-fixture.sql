-- Fixture inverse exclusivement pour une COPIE de validation, jamais une procédure de retour métier.
\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
  IF current_database() !~ '^transport_schema_copy_[a-z0-9_]+$' THEN
    RAISE EXCEPTION 'Fixture autorisée uniquement sur une copie de validation nommée.';
  END IF;
END $$;
ALTER TABLE agences RENAME COLUMN duree_trajet_minutes TO duree_match_minutes;
ALTER TABLE agences RENAME COLUMN duree_entre_trajet_minutes TO duree_entre_match_minutes;
ALTER TABLE vehicules RENAME COLUMN agence_id TO site_id;
ALTER TABLE voyageurs RENAME COLUMN agence_id TO site_id;
ALTER TABLE voyageurs RENAME COLUMN type_voyageur TO type_membre;
ALTER TABLE administrateurs RENAME COLUMN agence_id TO site_id;
ALTER TABLE indisponibilites_service RENAME COLUMN agence_id TO site_id;
ALTER TABLE trajets RENAME COLUMN vehicule_id TO terrain_id;
ALTER TABLE trajets RENAME COLUMN chauffeur_id TO organisateur_id;
ALTER TABLE trajets RENAME COLUMN type_trajet TO type_match;
ALTER TABLE trajets RENAME COLUMN nb_passagers_actuels TO nb_joueurs_actuels;
ALTER TABLE trajets RENAME COLUMN prix_par_passager TO prix_par_joueur;
ALTER TABLE reservations RENAME COLUMN trajet_id TO match_id;
ALTER TABLE reservations RENAME COLUMN voyageur_id TO membre_id;
ALTER TABLE restrictions RENAME COLUMN voyageur_id TO membre_id;
ALTER TABLE agences RENAME TO sites;
ALTER TABLE vehicules RENAME TO terrains;
ALTER TABLE voyageurs RENAME TO membres;
ALTER TABLE trajets RENAME TO matches;
ALTER TABLE restrictions RENAME TO penalites;
ALTER TABLE indisponibilites_service RENAME TO jours_fermeture;
COMMIT;
