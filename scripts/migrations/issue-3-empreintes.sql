-- Inventaire sans exporter les données personnelles : seules des empreintes sont retournées.
\set ON_ERROR_STOP on
CREATE OR REPLACE FUNCTION pg_temp.empreintes_transport()
RETURNS TABLE(nom text, lignes bigint, empreinte text, cles bigint, index_count bigint)
LANGUAGE plpgsql AS $$
DECLARE m record; physique text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('sites','agences'), ('terrains','vehicules'), ('membres','voyageurs'),
    ('matches','trajets'), ('penalites','restrictions'),
    ('jours_fermeture','indisponibilites_service'),
    ('reservations','reservations'),('paiements','paiements'),('administrateurs','administrateurs')
  ) AS mapping(ancien,nouveau) LOOP
    physique := CASE WHEN to_regclass('public.'||m.nouveau) IS NOT NULL THEN m.nouveau ELSE m.ancien END;
    nom := m.nouveau;
    EXECUTE format($q$
      SELECT count(*), md5(coalesce(string_agg(doc::text, E'\n' ORDER BY doc::text),''))
      FROM (SELECT (SELECT jsonb_object_agg(CASE e.key
        WHEN 'site_id' THEN 'agence_id' WHEN 'terrain_id' THEN 'vehicule_id'
        WHEN 'match_id' THEN 'trajet_id' WHEN 'membre_id' THEN 'voyageur_id'
        WHEN 'organisateur_id' THEN 'chauffeur_id' WHEN 'type_membre' THEN 'type_voyageur'
        WHEN 'type_match' THEN 'type_trajet' WHEN 'nb_joueurs_actuels' THEN 'nb_passagers_actuels'
        WHEN 'prix_par_joueur' THEN 'prix_par_passager'
        WHEN 'duree_match_minutes' THEN 'duree_trajet_minutes'
        WHEN 'duree_entre_match_minutes' THEN 'duree_entre_trajet_minutes'
        ELSE e.key END, e.value) FROM jsonb_each(to_jsonb(t)) e) doc FROM public.%I t) s
    $q$,physique) INTO lignes,empreinte;
    SELECT count(*) INTO cles FROM pg_constraint WHERE conrelid=to_regclass('public.'||physique);
    SELECT count(*) INTO index_count FROM pg_index WHERE indrelid=to_regclass('public.'||physique);
    RETURN NEXT;
  END LOOP;
END $$;
SELECT * FROM pg_temp.empreintes_transport() ORDER BY nom;
