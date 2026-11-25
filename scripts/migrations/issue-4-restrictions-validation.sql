-- Additions uniquement : aucun compte, paiement ou historique réécrit.
-- Sauvegarder la base avant toute application hors base de validation isolée.
ALTER TABLE penalites ADD COLUMN IF NOT EXISTS date_debut date;
ALTER TABLE penalites ADD COLUMN IF NOT EXISTS source_evenement varchar(255);
CREATE UNIQUE INDEX IF NOT EXISTS uq_penalite_source_evenement ON penalites(source_evenement);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS date_annulation timestamp;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS absence_declaree_le timestamp;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS motif_absence varchar(500);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS absence_admin_id bigint REFERENCES administrateurs(id);
