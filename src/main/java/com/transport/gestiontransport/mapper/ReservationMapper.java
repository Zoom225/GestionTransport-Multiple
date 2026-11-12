package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.response.ReservationResponse;
import com.transport.gestiontransport.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final PaiementMapper paiementMapper;

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        // Gestion des relations potentiellement nulles pour éviter les NullPointerExceptions
        if (reservation.getTrajet() == null || reservation.getVoyageur() == null) {
            // Il est préférable de lever une exception ou de retourner un DTO invalide
            // plutôt que de laisser une NullPointerException se produire.
            // Pour cet exemple, nous retournons null, mais une exception serait plus claire.
            throw new IllegalArgumentException("La réservation doit avoir un trajet et un voyageur associés.");
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .trajetId(reservation.getTrajet().getId())
                .trajetDateTime(reservation.getTrajet().getDateDebut())
                .voyageurId(reservation.getVoyageur().getId())
                .voyageurNom(reservation.getVoyageur().getPrenom() + " " + reservation.getVoyageur().getNom())
                .statut(reservation.getStatut())
                .paiement(reservation.getPaiement() != null
                        ? paiementMapper.toResponse(reservation.getPaiement())
                        : null)
                .build();
    }
}
