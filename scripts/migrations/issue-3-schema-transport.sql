-- Exécution explicite après sauvegarde/restauration vérifiée, application arrêtée.
-- Testé sur copie uniquement. Aucun INSERT/UPDATE/DELETE, aucun changement de type.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL lock_timeout = '5s';
SELECT pg_advisory_xact_lock(3042026);
DO $$
DECLARE m record;
BEGIN
  -- Refuser un état ambigu plutôt que fusionner deux jeux de données.
  FOR m IN SELECT * FROM (VALUES
    ('sites','agences'), ('terrains','vehicules'), ('membres','voyageurs'),
    ('matches','trajets'), ('penalites','restrictions'),
    ('jours_fermeture','indisponibilites_service')
  ) AS mapping(ancien,nouveau) LOOP
    IF to_regclass('public.' || m.ancien) IS NOT NULL AND to_regclass('public.' || m.nouveau) IS NOT NULL THEN
      RAISE EXCEPTION 'Tables source et cible présentes : % / %. Intervention manuelle requise.',m.ancien,m.nouveau;
    END IF;
    IF to_regclass('public.' || m.ancien) IS NULL AND to_regclass('public.' || m.nouveau) IS NULL THEN
      RAISE EXCEPTION 'Table attendue absente : % / %',m.ancien,m.nouveau;
    END IF;
  END LOOP;
  -- Les corps SQL textuels ne sont pas réécrits par ALTER TABLE RENAME.
  IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
    WHERE n.nspname='public' AND p.prosrc ~* '\m(sites|terrains|membres|matches|penalites|jours_fermeture)\M') THEN
    RAISE EXCEPTION 'Fonction SQL historique détectée : adaptation et revue nécessaires avant migration.';
  END IF;
  FOR m IN SELECT * FROM (VALUES
    ('sites','agences'), ('terrains','vehicules'), ('membres','voyageurs'),
    ('matches','trajets'), ('penalites','restrictions'),
    ('jours_fermeture','indisponibilites_service')
  ) AS mapping(ancien,nouveau) LOOP
    IF to_regclass('public.' || m.ancien) IS NOT NULL THEN
      EXECUTE format('ALTER TABLE public.%I RENAME TO %I',m.ancien,m.nouveau);
    END IF;
  END LOOP;
  FOR m IN SELECT * FROM (VALUES
    ('agences','duree_match_minutes','duree_trajet_minutes'),
    ('agences','duree_entre_match_minutes','duree_entre_trajet_minutes'),
    ('vehicules','site_id','agence_id'), ('voyageurs','site_id','agence_id'),
    ('voyageurs','type_membre','type_voyageur'), ('administrateurs','site_id','agence_id'),
    ('indisponibilites_service','site_id','agence_id'),
    ('trajets','terrain_id','vehicule_id'), ('trajets','organisateur_id','chauffeur_id'),
    ('trajets','type_match','type_trajet'), ('trajets','nb_joueurs_actuels','nb_passagers_actuels'),
    ('trajets','prix_par_joueur','prix_par_passager'),
    ('reservations','match_id','trajet_id'), ('reservations','membre_id','voyageur_id'),
    ('restrictions','membre_id','voyageur_id')
  ) AS mapping(tbl,ancien,nouveau) LOOP
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=m.tbl AND column_name=m.ancien) THEN
      IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=m.tbl AND column_name=m.nouveau) THEN
        RAISE EXCEPTION 'Colonnes source et cible présentes : %.%',m.tbl,m.nouveau;
      END IF;
      EXECUTE format('ALTER TABLE public.%I RENAME COLUMN %I TO %I',m.tbl,m.ancien,m.nouveau);
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=m.tbl AND column_name=m.nouveau) THEN
      RAISE EXCEPTION 'Colonne attendue absente : %.%',m.tbl,m.nouveau;
    END IF;
  END LOOP;
END $$;
COMMIT;
