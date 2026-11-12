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
    private LocalDateTime absenceDeclareeLe;
    private String motifAbsence;
    private Long trajetId;
    private LocalDateTime trajetDateTime;
    private Long voyageurId;
    private String voyageurNom;
    private StatutReservation statut;
    private PaiementResponse paiement;
    private com.transport.gestiontransport.entity.enums.VilleBelge villeDepart;
    private com.transport.gestiontransport.entity.enums.VilleBelge villeArrivee;
}
