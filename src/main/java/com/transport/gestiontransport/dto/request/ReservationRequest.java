package com.transport.gestiontransport.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull(message = "L'ID du trajet est obligatoire")
    private Long trajetId;

    @NotNull(message = "L'ID du voyageur est obligatoire")
    private Long voyageurId;

    // Compatibilité avec l'ancien client ; l'identité utilisée vient toujours du JWT.
    private Long requesterId;
}
