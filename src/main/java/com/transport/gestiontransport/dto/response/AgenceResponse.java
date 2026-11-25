package com.transport.gestiontransport.dto.response;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgenceResponse {
    private Long id;
    private String nom;
    private String adresse;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Integer dureeTrajetMinutes;
    private Integer dureeEntreTrajetMinutes;
    private Integer anneeCivile;
}
