package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.request.AgenceRequest;
import com.transport.gestiontransport.dto.response.AgenceResponse;
import com.transport.gestiontransport.entity.Agence;
import org.springframework.stereotype.Component;

@Component
public class AgenceMapper {

    public Agence toEntity(AgenceRequest request) {
        if (request == null) {
            return null;
        }
        return Agence.builder()
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .heureOuverture(request.getHeureOuverture())
                .heureFermeture(request.getHeureFermeture())
                .dureeTrajetMinutes(request.getDureeTrajetMinutes())
                .dureeEntreTrajetMinutes(request.getDureeEntreTrajetMinutes())
                .anneeCivile(request.getAnneeCivile())
                .build();
    }

    public AgenceResponse toResponse(Agence agence) {
        if (agence == null) {
            return null;
        }
        // et montre la robustesse. Le simple fait d'utiliser le builder sur une entité
        // avec des champs null ne lèvera pas de NullPointerException.
        // L'erreur venait probablement d'une entité Agence elle-même nulle dans la liste.
        // Ajouter cette vérification de nullité pour l'entité entière est la clé.
        return AgenceResponse.builder()
                .id(agence.getId())
                .nom(agence.getNom())
                .adresse(agence.getAdresse())
                .heureOuverture(agence.getHeureOuverture())
                .heureFermeture(agence.getHeureFermeture())
                .dureeTrajetMinutes(agence.getDureeTrajetMinutes())
                .dureeEntreTrajetMinutes(agence.getDureeEntreTrajetMinutes())
                .anneeCivile(agence.getAnneeCivile())
                .build();
    }
}
