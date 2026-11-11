package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.request.IndisponibiliteServiceRequest;
import com.transport.gestiontransport.dto.response.IndisponibiliteServiceResponse;
import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.IndisponibiliteService;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.IndisponibiliteServiceRepository;
import com.transport.gestiontransport.service.AgenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indisponibilites-service")
@RequiredArgsConstructor
@Tag(name = "Indisponibilités de service", description = "Endpoints de gestion des indisponibilités globales ou propres à une agence.")
public class IndisponibiliteServiceController {

    private final IndisponibiliteServiceRepository indisponibiliteServiceRepository;
    private final AgenceService agenceService;
    private final AdministrateurRepository administrateurRepository;

    @Operation(
            summary = "Lister les indisponibilités de service visibles",
            description = "Retourne les indisponibilités visibles pour l'administrateur connecté. " +
                    "Un admin GLOBAL voit tout ; un admin AGENCE voit les indisponibilités globales et celles de son agence.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping
    public ResponseEntity<List<IndisponibiliteServiceResponse>> getAll() {
        Administrateur admin = getAuthenticatedAdmin();
        return ResponseEntity.ok(
                indisponibiliteServiceRepository.findAll().stream()
                        .filter(jour -> canAccessJour(admin, jour))
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Lister les indisponibilités globales",
            description = "Retourne les indisponibilités qui bloquent toutes les agences. Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/global")
    public ResponseEntity<List<IndisponibiliteServiceResponse>> getGlobal() {
        return ResponseEntity.ok(
                indisponibiliteServiceRepository.findByGlobalTrue().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Lister les indisponibilités d'une agence",
            description = "Retourne les indisponibilités propres à une agence. " +
                    "Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/agence/{agenceId}")
    public ResponseEntity<List<IndisponibiliteServiceResponse>> getByAgence(
            @Parameter(description = "ID de cette agence", required = true)
            @PathVariable Long agenceId) {
        Administrateur admin = getAuthenticatedAdmin();
        validateAgenceAccess(admin, agenceId);
        return ResponseEntity.ok(
                indisponibiliteServiceRepository.findByAgenceId(agenceId).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Créer un indisponibilité de service",
            description = "Crée une indisponibilité globale ou une indisponibilité propre à une agence. " +
                    "Seul un admin GLOBAL peut créer une indisponibilité globale. " +
                    "Un admin AGENCE ne peut créer une indisponibilité que sur sa propre agence.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PostMapping
    public ResponseEntity<IndisponibiliteServiceResponse> create(@Valid @RequestBody IndisponibiliteServiceRequest request) {
        Administrateur admin = getAuthenticatedAdmin();

        if (Boolean.TRUE.equals(request.getGlobal())) {
            if (admin.getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
                throw new BusinessException("Seul un admin GLOBAL peut créer une indisponibilité globale.");
            }
        } else if (request.getAgenceId() == null) {
            throw new BusinessException("Un agence est obligatoire pour une indisponibilité non globale.");
        } else {
            validateAgenceAccess(admin, request.getAgenceId());
        }

        Agence agence = Boolean.TRUE.equals(request.getGlobal()) ? null : agenceService.getById(request.getAgenceId());
        IndisponibiliteService jour = IndisponibiliteService.builder()
                .date(request.getDate())
                .raison(request.getRaison())
                .global(Boolean.TRUE.equals(request.getGlobal()))
                .agence(agence)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(indisponibiliteServiceRepository.save(jour)));
    }

    @Operation(
            summary = "Supprimer un indisponibilité de service",
            description = "Supprime une indisponibilité. " +
                    "Seul un admin GLOBAL peut supprimer une indisponibilité globale. " +
                    "Un admin AGENCE ne peut supprimer que les indisponibilités de son agence.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du indisponibilité de service", required = true)
            @PathVariable Long id) {
        Administrateur admin = getAuthenticatedAdmin();
        IndisponibiliteService jour = indisponibiliteServiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Jour de indisponibilité introuvable."));

        if (Boolean.TRUE.equals(jour.getGlobal()) && admin.getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
            throw new BusinessException("Seul un admin GLOBAL peut supprimer une indisponibilité globale.");
        }
        if (jour.getAgence() != null) {
            validateAgenceAccess(admin, jour.getAgence().getId());
        }

        indisponibiliteServiceRepository.delete(jour);
        return ResponseEntity.noContent().build();
    }

    private Administrateur getAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification administrateur requise.");
        }

        return administrateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Administrateur introuvable."));
    }

    private boolean canAccessJour(Administrateur admin, IndisponibiliteService jour) {
        return admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL
                || Boolean.TRUE.equals(jour.getGlobal())
                || (jour.getAgence() != null && admin.getAgence() != null && jour.getAgence().getId().equals(admin.getAgence().getId()));
    }

    private void validateAgenceAccess(Administrateur admin, Long agenceId) {
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (admin.getAgence() == null || !admin.getAgence().getId().equals(agenceId)) {
            throw new BusinessException("Un admin AGENCE ne peut agir que sur sa propre agence.");
        }
    }

    private IndisponibiliteServiceResponse toResponse(IndisponibiliteService jour) {
        return new IndisponibiliteServiceResponse(
                jour.getId(),
                jour.getDate(),
                jour.getRaison(),
                jour.getGlobal(),
                jour.getAgence() != null ? jour.getAgence().getId() : null,
                jour.getAgence() != null ? jour.getAgence().getNom() : null
        );
    }
}
