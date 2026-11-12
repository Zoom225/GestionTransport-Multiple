package com.transport.gestiontransport.dto.response;

import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String nom;
    private String prenom;
    private TypeAdministrateur role;
    private Long agenceId;
}
