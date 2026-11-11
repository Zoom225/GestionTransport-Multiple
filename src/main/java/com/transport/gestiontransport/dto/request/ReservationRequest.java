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

    @NotNull(message = "L'ID du demandeur est obligatoire")
    private Long requesterId;
}
