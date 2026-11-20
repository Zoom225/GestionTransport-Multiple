-- Additive only. Historical date-only restrictions are deliberately not reinterpreted.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE restrictions ADD COLUMN IF NOT EXISTS debut_suspension timestamptz;
ALTER TABLE restrictions ADD COLUMN IF NOT EXISTS fin_suspension timestamptz;
ALTER TABLE restrictions ADD COLUMN IF NOT EXISTS trajet_origine_id bigint;
ALTER TABLE restrictions ADD COLUMN IF NOT EXISTS montant_penalite numeric(12,2);
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='restrictions'::regclass AND conname='fk_restriction_trajet_origine') THEN
        ALTER TABLE restrictions ADD CONSTRAINT fk_restriction_trajet_origine FOREIGN KEY(trajet_origine_id) REFERENCES trajets(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='restrictions'::regclass AND conname='ck_suspension_168h') THEN
        ALTER TABLE restrictions ADD CONSTRAINT ck_suspension_168h CHECK (
            (debut_suspension IS NULL AND fin_suspension IS NULL)
            OR (debut_suspension IS NOT NULL AND fin_suspension IS NOT NULL
                AND fin_suspension = debut_suspension + interval '168 hours'));
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uq_restrictions_trajet_origine ON restrictions(trajet_origine_id);
CREATE INDEX IF NOT EXISTS idx_restrictions_suspension ON restrictions(voyageur_id,fin_suspension);
COMMIT;
