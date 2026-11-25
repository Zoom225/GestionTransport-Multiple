\set ON_ERROR_STOP on
DO $$
BEGIN
    IF (SELECT count(*) FROM information_schema.columns
        WHERE table_schema = 'public' AND (table_name, column_name) IN (
            ('paiements','montant'), ('trajets','prix_total'), ('trajets','prix_par_passager'),
            ('vehicules','prix'), ('voyageurs','solde')
        ) AND data_type = 'numeric' AND numeric_precision = 19 AND numeric_scale = 2) <> 5 THEN
        RAISE EXCEPTION 'Cinq colonnes monétaires décimales attendues';
    END IF;
    IF (SELECT ARRAY(SELECT solde FROM voyageurs ORDER BY id)) IS DISTINCT FROM ARRAY[123.45, 0.00]
       OR (SELECT ARRAY(SELECT prix FROM vehicules ORDER BY id)) IS DISTINCT FROM ARRAY[89.99, NULL]
       OR NOT EXISTS (SELECT FROM trajets WHERE id = 1 AND vehicule_id = 1 AND prix_total = 100.00
                      AND prix_par_passager = 25.00)
       OR NOT EXISTS (SELECT FROM paiements WHERE id = 1 AND trajet_id = 1 AND montant = 25.00) THEN
        RAISE EXCEPTION 'Valeurs, identifiants ou relations monétaires non préservés';
    END IF;
END $$;
