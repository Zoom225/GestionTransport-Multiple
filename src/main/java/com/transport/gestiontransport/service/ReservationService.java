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
    void confirm(Long reservationId);
    Reservation preparePaiement(Long reservationId, Long voyageurId);
    void expirePendingReservations();
    void cancelTrajet(Long trajetId, Long chauffeurId);
}
