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
    void confirm(Long reservationId);
}
