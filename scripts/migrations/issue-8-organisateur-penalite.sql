-- Additive migration: no attribution of historical trips or debt reassignment.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE trajets ADD COLUMN IF NOT EXISTS voyageur_organisateur_id bigint;
ALTER TABLE trajets ADD COLUMN IF NOT EXISTS penalite_incomplet_imputee_le timestamp;
ALTER TABLE trajets ADD COLUMN IF NOT EXISTS penalite_incomplet_payee_le timestamp;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'trajets'::regclass
                   AND conname = 'fk_trajets_organisateur') THEN
        ALTER TABLE trajets ADD CONSTRAINT fk_trajets_organisateur
            FOREIGN KEY (voyageur_organisateur_id) REFERENCES voyageurs(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_trajets_organisateur ON trajets(voyageur_organisateur_id);
COMMIT;
