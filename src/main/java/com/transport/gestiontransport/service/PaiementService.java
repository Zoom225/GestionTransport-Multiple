package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Paiement;

import java.util.List;

public interface PaiementService {

    /**
     * Règle métier : Confirmation de réservation par le paiement (modèle simple).
     * <p>
     * Principe « Premier payé, premier servi » :
     * <ul>
     *   <li>Le paiement d'une réservation (25 €) déclenche immédiatement la confirmation de la place à bord.</li>
     *   <li>Message utilisateur associé : « Paiement validé : votre réservation est confirmée. »</li>
     *   <li>Enregistre le paiement au statut {@code PAYE} avec horodatage.</li>
     *   <li>Bascule la réservation au statut {@code CONFIRMEE} et incrémente le nombre de passagers du trajet.</li>
     * </ul>
     *
     * @param reservationId identifiant de la réservation à payer
     * @param voyageurId identifiant du voyageur payeur (doit être le titulaire)
     * @return le paiement validé
     */
    Paiement pay(Long reservationId, Long voyageurId);

    Paiement getById(Long id);

    Paiement getByReservationId(Long reservationId);

    List<Paiement> getByVoyageurId(Long voyageurId);

    void checkPaiementOwner(Long paiementId, Long voyageurId);

    void checkReservationPaiementOwner(Long reservationId, Long voyageurId);

    void checkUnpaidBeforeTrajet();
}
