-- Additive audit field. Existing cancellations retain their original history.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE trajets ADD COLUMN IF NOT EXISTS annule_par_voyageur_id bigint;
COMMIT;
