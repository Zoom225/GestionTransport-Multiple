package com.transport.gestiontransport.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndisponibiliteServiceRequest {

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    private String raison;

    @NotNull(message = "Le type de indisponibilité est obligatoire")
    private Boolean global;

    private Long agenceId;
}
