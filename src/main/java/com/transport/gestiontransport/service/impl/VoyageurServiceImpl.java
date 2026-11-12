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
// règle metier addition restriction à organisateur du trajet privée
    @Override
    public void addRestriction(Long voyageurId) {
        Voyageur voyageur = getById(voyageurId);
        Restriction restriction = Restriction.builder()
                .voyageur(voyageur)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Trajet privé incomplet avant l'échéance")
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

    private String normalizeMatricule(String matricule) {
        return matricule == null ? "" : matricule.trim().toUpperCase(Locale.ROOT);
    }

    private void validateMatricule(String matricule, TypeVoyageur type) {
        boolean valid = switch (type) {
            case VIP -> matricule.matches("^G\\d{4}$");
            case LOCAL -> matricule.matches("^S\\d{5}$");
            case LIBRE  -> matricule.matches("^L\\d{5}$");
        };
        // Regle metier : un matricule doit respecter le format du type de voyageur.
        if (!valid) {
            throw new BusinessException("Format de matricule invalide pour le type " + type + " : " + matricule);
        }
    }
}
