package com.transport.gestiontransport.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record CreateTrajetRequest(
    @NotNull(message = "L'ID du vehicule est obligatoire")
    Long vehiculeId,

    @NotNull(message = "La date et l'heure du trajet sont obligatoires")
    @FutureOrPresent(message = "La date du trajet doit etre dans le futur ou le present")
    LocalDateTime trajetDate,

    @NotBlank(message = "Le type de trajet est obligatoire")
    @Pattern(regexp = "PUBLIC|PRIVE", message = "Le type de trajet doit etre PUBLIC ou PRIVE")
    String trajetType // PUBLIC ou PRIVE
) {}
