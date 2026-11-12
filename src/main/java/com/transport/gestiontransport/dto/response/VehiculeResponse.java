package com.transport.gestiontransport.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeResponse {
    private Long id;
    private String nom;
    private Long agenceId;
    private String agenceNom;
    private Double prix;
}
