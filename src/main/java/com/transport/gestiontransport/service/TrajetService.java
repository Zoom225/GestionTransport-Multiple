package com.transport.gestiontransport.service;

import com.transport.gestiontransport.dto.request.TrajetRequest;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.dto.request.CreateTrajetRequest;
import com.transport.gestiontransport.dto.response.TrajetDto;

import java.util.List;

public interface TrajetService {

    // Règle héritée du socle : quatre passagers. À remplacer par la capacité du véhicule lors de la phase métier.
    int REQUIRED_PASSAGERS_PER_TRAJET = 4;
    double PRIX_TRAJET_EUROS = 100.0;

    TrajetDto createTrajet(CreateTrajetRequest request, String username);

    List<TrajetDto> findAllTrajets();

    List<TrajetDto> getPublicAvailableTrajets();

    List<TrajetDto> findByOrganisateur(Long organisateurId);

    List<TrajetDto> findByAgence(Long agenceId);

    Trajet getTrajetEntityById(Long id);

    TrajetDto getTrajetDtoById(Long id);

    TrajetDto updateTrajet(Long trajetId, TrajetRequest request);

    void cancelTrajet(Long trajetId, Long requesterId);

    void convertToPublic(Long trajetId);

    void incrementPassagers(Long trajetId);

    void decrementPassagers(Long trajetId);

    void checkAndConvertExpiredPrivateTrajets();
}
