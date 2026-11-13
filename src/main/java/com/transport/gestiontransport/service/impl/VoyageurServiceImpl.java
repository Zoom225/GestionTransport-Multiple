package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Restriction;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.VoyageurRepository;
import com.transport.gestiontransport.repository.RestrictionRepository;
import com.transport.gestiontransport.service.VoyageurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VoyageurServiceImpl implements VoyageurService {

    private final VoyageurRepository voyageurRepository;
    private final RestrictionRepository restrictionRepository;
    private final PasswordEncoder passwordEncoder; // Added PasswordEncoder
    @org.springframework.beans.factory.annotation.Value("${transport.restriction.duree-jours:7}")
    private int dureeRestrictionJours = 7;

    @Override
    public Voyageur create(Voyageur voyageur) {
        voyageur.setMatricule(normalizeMatricule(voyageur.getMatricule()));
        validateMatricule(voyageur.getMatricule(), voyageur.getTypeVoyageur());

        // Regle metier : le matricule doit etre unique.
        if (voyageurRepository.existsByMatricule(voyageur.getMatricule())) {
            throw new BusinessException("Le matricule existe déjà : " + voyageur.getMatricule());
        }
        // Regle metier : l'email doit etre unique lorsqu'il est renseigne.
        if (voyageur.getEmail() != null && voyageurRepository.existsByEmail(voyageur.getEmail())) {
            throw new BusinessException("L'email existe déjà : " + voyageur.getEmail());
        }
        // Regle metier : un voyageur AGENCE doit etre rattache a une agence.
        if (voyageur.getTypeVoyageur() == TypeVoyageur.LOCAL && voyageur.getAgence() == null) {
            throw new BusinessException("Un voyageur AGENCE doit être lié à une agence.");
        }

        voyageur.setSolde(0.0);
        voyageur.setPeutProposerTrajet(Boolean.TRUE.equals(voyageur.getPeutProposerTrajet()));
        return voyageurRepository.save(voyageur);
    }

    @Override
    public Voyageur getById(Long id) {
        return voyageurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable avec l'ID : " + id));
    }

    @Override
    public Voyageur getByMatricule(String matricule) {
        String normalizedMatricule = normalizeMatricule(matricule);
        return voyageurRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable avec le matricule : " + normalizedMatricule));
    }

    @Override
    public List<Voyageur> getAll() {
        return voyageurRepository.findAll();
    }

    @Override
    public Voyageur update(Long id, Voyageur voyageur) {
        Voyageur existing = getById(id);
        existing.setNom(voyageur.getNom());
        existing.setPrenom(voyageur.getPrenom());
        existing.setEmail(voyageur.getEmail());
        if (voyageur.getPeutProposerTrajet() != null) existing.setPeutProposerTrajet(voyageur.getPeutProposerTrajet());
        return voyageurRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Voyageur existing = getById(id);
        voyageurRepository.delete(existing);
    }

    @Override
    public boolean hasActiveRestriction(Long voyageurId) {
        return restrictionRepository.existsByVoyageurIdAndDateFinAfter(voyageurId, LocalDate.now());
    }

    @Override
    public boolean hasOutstandingBalance(Long voyageurId) {
        Voyageur voyageur = getById(voyageurId);
        return voyageur.getSolde() > 0.0;
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void addRestriction(Long voyageurId) {
        Voyageur voyageur = voyageurRepository.findByIdForUpdate(voyageurId)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable."));
        if (hasActiveRestriction(voyageurId)) return;
        if (dureeRestrictionJours < 1) throw new BusinessException("La durée de restriction doit être positive.");
        Restriction restriction = Restriction.builder()
                .voyageur(voyageur)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(dureeRestrictionJours))
                .motif("Suspension de réservation après décision métier validée")
                .build();
        restrictionRepository.save(restriction);
    }

    @Override
    public Voyageur authenticate(String matricule, String password) {
        String normalizedMatricule = normalizeMatricule(matricule);
        Voyageur voyageur = voyageurRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new BusinessException("Identifiants invalides."));

        if (!StringUtils.hasText(password)
                || !StringUtils.hasText(voyageur.getPasswordHash())
                || !passwordEncoder.matches(password, voyageur.getPasswordHash())) {
            throw new BusinessException("Identifiants invalides.");
        }

        return voyageur;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void addRestrictionPourEvenement(Long voyageurId, String source, String motif) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(motif) || source.length() > 255 || motif.length() > 255) {
            throw new BusinessException("Le motif et la référence de la restriction sont obligatoires (255 caractères maximum).");
        }
        Voyageur voyageur = voyageurRepository.findByIdForUpdate(voyageurId)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable."));
        if (restrictionRepository.existsBySourceEvenement(source)) return;
        if (dureeRestrictionJours < 1) throw new BusinessException("La durée de restriction doit être positive.");
        restrictionRepository.save(Restriction.builder().voyageur(voyageur).sourceEvenement(source)
                .motif(motif).dateDebut(LocalDate.now()).dateFin(LocalDate.now().plusDays(dureeRestrictionJours)).build());
    }

    @Override
    public List<Restriction> getRestrictions(Long voyageurId) {
        getById(voyageurId);
        return restrictionRepository.findByVoyageurIdOrderByDateFinDescIdDesc(voyageurId);
    }

    private String normalizeMatricule(String matricule) {
        return matricule == null ? "" : matricule.trim().toUpperCase(Locale.ROOT);
    }

    private void validateMatricule(String matricule, TypeVoyageur type) {
        boolean valid = switch (type) {
            case VIP -> matricule.matches("^[VG]\\d{4}$");
            case LOCAL -> matricule.matches("^[LS]\\d{5}$");
            case LIBRE  -> matricule.matches("^[FL]\\d{5}$");
        };
        // Regle metier : un matricule doit respecter le format du type de voyageur.
        if (!valid) {
            throw new BusinessException("Format de matricule invalide pour le type " + type + " : " + matricule);
        }
    }
}
