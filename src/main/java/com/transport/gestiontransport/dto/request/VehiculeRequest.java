package com.transport.gestiontransport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    private String nom;

    @NotNull(message = "L'ID de cette agence est obligatoire.")
    private Long agenceId;
}
