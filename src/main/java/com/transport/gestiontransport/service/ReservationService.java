package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Reservation;

import java.util.List;

public interface ReservationService {
    Reservation create(Long trajetId, Long voyageurId, Long requesterId);
    List<Reservation> createForInvites(Long trajetId, Long requesterId, List<String> inviteeMatricules);
    Reservation getById(Long id);
    List<Reservation> getByTrajetId(Long trajetId);
    List<Reservation> getByVoyageurId(Long voyageurId);
    void checkReservationOwner(Long reservationId, Long voyageurId);
    void cancel(Long reservationId);
    void cancelForUser(Long reservationId, Long voyageurId);

    /**
     * Confirme définitivement la réservation d'une place client après validation de son paiement (25 €).
     * Règle « Premier payé, premier servi » : incrémente le nombre de passagers du trajet et marque
     * le trajet COMPLET à 4 passagers payés.
     *
     * @param reservationId identifiant de la réservation payée
     */
    void confirm(Long reservationId);

    /**
     * Prépare et valide l'éligibilité d'une réservation pour le paiement par son titulaire.
     *
     * @param reservationId identifiant de la réservation en attente
     * @param voyageurId identifiant du voyageur connecté qui effectue le paiement
     * @return la réservation vérifiée prête pour le paiement
     */
    Reservation preparePaiement(Long reservationId, Long voyageurId);

    void expirePendingReservations();

    /**
     * Annule le trajet entier ainsi que toutes les réservations actives associées.
     * Accessible à l'organisateur responsable ou au chauffeur affecté uniquement avant le départ.
     *
     * @param trajetId identifiant du trajet
     * @param chauffeurId identifiant du voyageur demandeur (organisateur ou chauffeur)
     */
    void cancelTrajet(Long trajetId, Long chauffeurId);

    Reservation declarerAbsence(Long reservationId, String motif);
}
