package com.transport.gestiontransport.dto.response;

import com.transport.gestiontransport.entity.enums.StatutPaiement;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementResponse {
    private Long id;
    private Double montant;
    private StatutPaiement statut;
    private LocalDateTime datePaiement;
}
