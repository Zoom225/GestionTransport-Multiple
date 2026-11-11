package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.request.VehiculeRequest;
import com.transport.gestiontransport.dto.response.VehiculeResponse;
import com.transport.gestiontransport.entity.Vehicule;
import org.springframework.stereotype.Component;

@Component
public class VehiculeMapper {

    public Vehicule toEntity(VehiculeRequest request) {
        return Vehicule.builder()
                .nom(request.getNom())
                .build();
    }

    public VehiculeResponse toResponse(Vehicule vehicule) {
        return VehiculeResponse.builder()
                .id(vehicule.getId())
                .nom(vehicule.getNom())
                .agenceId(vehicule.getAgence() != null ? vehicule.getAgence().getId() : null)
                .agenceNom(vehicule.getAgence() != null ? vehicule.getAgence().getNom() : null)
                .prix(vehicule.getPrix())
                .build();
    }
}
