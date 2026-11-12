package com.transport.gestiontransport.dto.request;

import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoyageurRequest {

    @NotBlank(message = "Le matricule est obligatoire.")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire.")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    private String prenom;

    @Email(message = "Le format de l'e-mail est invalide.")
    private String email;

    @NotNull(message = "Le type de voyageur est obligatoire.")
    private TypeVoyageur typeVoyageur;

    private Long agenceId;

    private String password;
    private Boolean peutProposerTrajet;
}
