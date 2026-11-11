package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.dto.response.TrajetDto;
import org.springframework.stereotype.Service;

@Service
public class TrajetMapper {

    public TrajetDto toTrajetDto(Trajet trajet) {
        if (trajet == null) {
            return null;
        }

        Vehicule vehicule = trajet.getVehicule();
        Voyageur organisateur = trajet.getOrganisateur();
        Agence agence = (vehicule != null) ? vehicule.getAgence() : null;

        Long vehiculeId = (vehicule != null) ? vehicule.getId() : null;
        String vehiculeNom = (vehicule != null) ? vehicule.getNom() : null;
        String agenceNom = (agence != null) ? agence.getNom() : null;
        Long organisateurId = (organisateur != null) ? organisateur.getId() : null;
        String organisateurNom = (organisateur != null && organisateur.getPrenom() != null && organisateur.getNom() != null)
                ? organisateur.getPrenom() + " " + organisateur.getNom()
                : null;

        return new TrajetDto(
                trajet.getId(),
                vehiculeId,
                vehiculeNom,
                agenceNom,
                organisateurId,
                organisateurNom,
                trajet.getDateDebut() != null ? trajet.getDateDebut().toLocalDate() : null,
                trajet.getDateDebut() != null ? trajet.getDateDebut().toLocalTime() : null,
                trajet.getDateFin() != null ? trajet.getDateFin().toLocalTime() : null,
                trajet.getTypeTrajet(),
                trajet.getStatut(),
                trajet.getNbPassagersActuels(),
                trajet.getPrixParPassager(),
                trajet.getDateConversionPublic()
        );
    }
}
