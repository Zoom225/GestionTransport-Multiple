-- Appliquer sur une copie validée, puis lors d’un arrêt applicatif autorisé.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL lock_timeout = '5s';
SET LOCAL extra_float_digits = 3;
LOCK TABLE paiements, trajets, vehicules, voyageurs IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    champ record;
    type_source text;
    incompatible boolean;
BEGIN
    FOR champ IN SELECT * FROM (VALUES
        ('paiements', 'montant'),
        ('trajets', 'prix_total'),
        ('trajets', 'prix_par_passager'),
        ('vehicules', 'prix'),
        ('voyageurs', 'solde')
    ) AS champs(t, c) LOOP
        SELECT format_type(a.atttypid, a.atttypmod)
          INTO type_source
          FROM pg_attribute a
         WHERE a.attrelid = to_regclass(format('public.%I', champ.t))
           AND a.attname = champ.c AND a.attnum > 0 AND NOT a.attisdropped;
        IF type_source IS NULL OR (type_source NOT IN ('double precision', 'real', 'numeric')
                AND type_source NOT LIKE 'numeric(%') THEN
            RAISE EXCEPTION 'Type monétaire inattendu pour %.% : %', champ.t, champ.c, type_source;
        END IF;
        EXECUTE format($query$
            SELECT EXISTS (
                SELECT FROM public.%1$I
                 WHERE CASE
                     WHEN %2$I IS NULL THEN FALSE
                     WHEN %2$I::text IN ('NaN', 'Infinity', '-Infinity') THEN TRUE
                     ELSE %2$I::text::numeric <> round(%2$I::text::numeric, 2)
                          OR abs(%2$I::text::numeric) >= 100000000000000000
                 END
            )$query$, champ.t, champ.c) INTO incompatible;
        IF incompatible THEN
            RAISE EXCEPTION 'Un montant de %.% ne peut pas être conservé à deux décimales. Migration annulée.',
                champ.t, champ.c;
        END IF;
        IF type_source = 'numeric(19,2)' THEN
            CONTINUE;
        END IF;
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I TYPE numeric(19,2) USING (%I::text::numeric)',
            champ.t, champ.c, champ.c);
    END LOOP;
END $$;
COMMIT;
