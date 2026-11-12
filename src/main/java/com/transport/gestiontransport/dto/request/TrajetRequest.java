package com.transport.gestiontransport.dto.request;

import com.transport.gestiontransport.entity.enums.TypeTrajet;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrajetRequest {

    @NotNull(message = "L'ID du vehicule est obligatoire.")
    private Long vehiculeId;

    @NotNull(message = "L'ID de l'organisateur est obligatoire.")
    private Long organisateurId;

    @NotNull(message = "La date est obligatoire.")
    @Future(message = "La date du trajet doit être dans le futur.")
    private LocalDate date;

    @NotNull(message = "L'heure de début est obligatoire.")
    private LocalTime heureDebut;

    @NotNull(message = "Le type de trajet est obligatoire.")
    private TypeTrajet typeTrajet;
}
