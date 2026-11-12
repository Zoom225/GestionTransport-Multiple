package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.dto.response.TrajetDto;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final AdministrateurRepository administrateurRepository;
    private final TrajetRepository trajetRepository;

    public Administrateur currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Regle metier : une action admin exige une authentification administrateur.
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification administrateur requise.");
        }

        return administrateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Administrateur introuvable."));
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public void requireGlobalAdmin() {
        // Regle metier : certaines actions sont reservees a l'admin GLOBAL.
        if (currentAdmin().getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
            throw new BusinessException("Action réservée à un admin GLOBAL.");
        }
    }

    public void checkAgenceAccess(Long agenceId) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        // Regle metier : un admin AGENCE ne peut agir que sur sa propre agence.
        if (admin.getAgence() == null || !admin.getAgence().getId().equals(agenceId)) {
            throw new BusinessException("Un admin AGENCE ne peut agir que sur sa propre agence.");
        }
    }

    public void checkVehiculeAccess(Vehicule vehicule) {
        checkAgenceAccess(vehicule.getAgence().getId());
    }    public void checkTrajetAccess(Long trajetId) {
        Trajet trajet = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new BusinessException("Trajet introuvable avec l'ID : " + trajetId));
        checkAgenceAccess(trajet.getVehicule().getAgence().getId());
    }

    public void checkVoyageurAccess(Voyageur voyageur) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        // Regle metier : un admin AGENCE ne peut gerer que les voyageurs de son agence.
        if (voyageur.getAgence() == null || admin.getAgence() == null || !voyageur.getAgence().getId().equals(admin.getAgence().getId())) {
            throw new BusinessException("Un admin AGENCE ne peut gérer que les voyageurs de son agence.");
        }
    }

    public List<Voyageur> filterVoyageurs(List<Voyageur> voyageurs) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return voyageurs;
        }
        Long agenceId = admin.getAgence() != null ? admin.getAgence().getId() : null;
        return voyageurs.stream()
                .filter(voyageur -> voyageur.getAgence() != null && voyageur.getAgence().getId().equals(agenceId))
                .toList();
    }

    public List<Trajet> filterTrajets(List<Trajet> trajets) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return trajets;
        }
        Long agenceId = admin.getAgence() != null ? admin.getAgence().getId() : null;
        return trajets.stream()
                .filter(trajet -> trajet.getVehicule() != null
                        && trajet.getVehicule().getAgence() != null
                        && trajet.getVehicule().getAgence().getId().equals(agenceId))
                .toList();
    }

    public List<TrajetDto> filterTrajetDtos(List<TrajetDto> trajets) {
        Administrateur admin = currentAdmin();
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return trajets;
        }
        Long agenceId = admin.getAgence() != null ? admin.getAgence().getId() : null;
        return trajets.stream()
                .filter(trajet -> trajet.agenceId() != null && trajet.agenceId().equals(agenceId))
                .toList();
    }
}
