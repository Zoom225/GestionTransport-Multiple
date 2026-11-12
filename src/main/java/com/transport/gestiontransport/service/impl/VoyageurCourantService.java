package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.VoyageurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoyageurCourantService {

    private final VoyageurRepository voyageurRepository;

    public Voyageur currentVoyageur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Regle metier : une action voyageur exige une authentification voyageur.
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification voyageur requise.");
        }

        return voyageurRepository.findByMatriculeIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException("Voyageur connecté introuvable."));
    }

    public Long currentVoyageurId() {
        return currentVoyageur().getId();
    }

    public void requireCurrentVoyageur(Long voyageurId) {
        // Regle metier : un voyageur ne peut agir que sur ses propres ressources.
        if (!currentVoyageurId().equals(voyageurId)) {
            throw new BusinessException("Accès refusé : cette ressource appartient à un autre voyageur.");
        }
    }
}
