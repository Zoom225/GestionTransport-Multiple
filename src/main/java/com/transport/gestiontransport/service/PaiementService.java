package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Paiement;

import java.util.List;

public interface PaiementService {
    Paiement pay(Long reservationId, Long voyageurId);
    Paiement getById(Long id);
    Paiement getByReservationId(Long reservationId);
    List<Paiement> getByVoyageurId(Long voyageurId);
    void checkPaiementOwner(Long paiementId, Long voyageurId);
    void checkReservationPaiementOwner(Long reservationId, Long voyageurId);
    void checkUnpaidBeforeTrajet();
}
