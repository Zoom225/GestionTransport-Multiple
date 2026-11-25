-- À appliquer explicitement après sauvegarde, jamais automatiquement au démarrage.
-- Aucun paiement ni aucune réservation existante n'est réécrit.
BEGIN;
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_statut_check;
ALTER TABLE reservations ADD CONSTRAINT reservations_statut_check
    CHECK (statut IN ('EN_ATTENTE', 'CONFIRMEE', 'ANNULEE', 'EXPIREE'));
COMMIT;
