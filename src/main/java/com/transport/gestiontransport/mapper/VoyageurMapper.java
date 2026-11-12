package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.request.VoyageurRequest;
import com.transport.gestiontransport.dto.response.VoyageurResponse;
import com.transport.gestiontransport.entity.Voyageur;
import org.springframework.stereotype.Component;

@Component
public class VoyageurMapper {

    public Voyageur toEntity(VoyageurRequest request) {
        return Voyageur.builder()
                .matricule(request.getMatricule())
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .typeVoyageur(request.getTypeVoyageur())
                .peutProposerTrajet(request.getPeutProposerTrajet())
                .build();
        // cette agence est résolu dans le service via agenceId
    }

    public VoyageurResponse toResponse(Voyageur voyageur) {
        return VoyageurResponse.builder()
                .id(voyageur.getId())
                .matricule(voyageur.getMatricule())
                .nom(voyageur.getNom())
                .prenom(voyageur.getPrenom())
                .email(voyageur.getEmail())
                .typeVoyageur(voyageur.getTypeVoyageur())
                .peutProposerTrajet(voyageur.getPeutProposerTrajet())
                .agenceId(voyageur.getAgence() != null ? voyageur.getAgence().getId() : null)
                .agenceNom(voyageur.getAgence() != null ? voyageur.getAgence().getNom() : null)
                .solde(voyageur.getSolde())
                .build();
    }
}
