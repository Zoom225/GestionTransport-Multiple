package com.transport.gestiontransport.dto.response;

import com.transport.gestiontransport.entity.enums.StatutReservation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private Long trajetId;
    private LocalDateTime trajetDateTime;
    private Long voyageurId;
    private String voyageurNom;
    private StatutReservation statut;
    private PaiementResponse paiement;
}
