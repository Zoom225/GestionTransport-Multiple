package com.transport.gestiontransport.dto.response;

import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoyageurResponse {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private TypeVoyageur typeVoyageur;
    private Long agenceId;
    private String agenceNom;
    private Double solde;
    private String token;
    private Boolean peutProposerTrajet;
}
