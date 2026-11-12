package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.request.TrajetRequest;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.dto.request.CreateTrajetRequest;
import com.transport.gestiontransport.dto.response.TrajetDto;
import com.transport.gestiontransport.service.TrajetService;
import com.transport.gestiontransport.service.impl.AdminAuthorizationService;
import com.transport.gestiontransport.service.impl.VoyageurCourantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trajets", description = "Endpoints de gestion des trajets de transport.")
public class TrajetController {

    private final TrajetService trajetService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final VoyageurCourantService currentVoyageurService;

    @Operation(
            summary = "Créer un trajet",
            description = "Crée un trajet PUBLIC ou PRIVÉ pour le voyageur connecté. " +
                    "Les limites de réservation GLOBAL, AGENCE et LIBRE s'appliquent à la création du trajet. " +
                    "Accessible uniquement à un voyageur authentifié.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PostMapping
    public ResponseEntity<TrajetDto> createTrajet(@Valid @RequestBody CreateTrajetRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("Requête de création de trajet reçue de l'utilisateur '{}' pour le vehicule ID {}", username, request.vehiculeId());

        if (username == null || "anonymousUser".equals(username)) {
            log.warn("Tentative de création de trajet par un utilisateur non authentifié.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        TrajetDto createdTrajet = trajetService.createTrajet(request, username);
        return new ResponseEntity<>(createdTrajet, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Lister tous les trajets",
            description = "Retourne les trajets visibles pour l'administrateur connecté. " +
                    "Un admin GLOBAL voit tous les trajets ; un admin AGENCE voit uniquement ceux de son agence.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping
    public ResponseEntity<List<TrajetDto>> getAllTrajets() {
        return ResponseEntity.ok(adminAuthorizationService.filterTrajetDtos(trajetService.findAllTrajets()));
    }

    @Operation(
            summary = "Lister les trajets publics disponibles",
            description = "Retourne les trajets PUBLICS planifiés et encore disponibles. Endpoint public."
    )
    @GetMapping("/public")
    public ResponseEntity<List<TrajetDto>> getPublicTrajets() {
        return ResponseEntity.ok(trajetService.getPublicAvailableTrajets());
    }

    @Operation(
            summary = "Consulter un trajet",
            description = "Retourne le détail d'un trajet. Accessible aux voyageurs et aux administrateurs authentifiés.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/{id}")
    public ResponseEntity<TrajetDto> getById(
            @Parameter(description = "ID du trajet", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(trajetService.getTrajetDtoById(id));
    }

    @Operation(
            summary = "Lister les trajets organisés par un voyageur",
            description = "Retourne les trajets organisés par le voyageur connecté. " +
                    "Le voyageur connecté doit correspondre à l'ID fourni.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/organisateur/{organisateurId}")
    public ResponseEntity<List<TrajetDto>> getByOrganisateur(
            @Parameter(description = "ID de l'organisateur", required = true)
            @PathVariable Long organisateurId) {
        currentVoyageurService.requireCurrentVoyageur(organisateurId);
        return ResponseEntity.ok(trajetService.findByOrganisateur(organisateurId));
    }

    @Operation(
            summary = "Lister les trajets d'une agence",
            description = "Retourne les trajets d'une agence. Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/agence/{agenceId}")
    public ResponseEntity<List<TrajetDto>> getByAgence(
            @Parameter(description = "ID de cette agence", required = true)
            @PathVariable Long agenceId) {
        adminAuthorizationService.checkAgenceAccess(agenceId);
        return ResponseEntity.ok(trajetService.findByAgence(agenceId));
    }

    @Operation(
            summary = "Modifier un trajet",
            description = "Modifie un trajet existant. Accessible uniquement au voyageur organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<TrajetDto> updateTrajet(
            @Parameter(description = "ID du trajet", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TrajetRequest request) {
        return ResponseEntity.ok(trajetService.updateTrajet(id, request));
    }

    @Operation(
            summary = "Annuler un trajet",
            description = "Annule un trajet existant. Accessible uniquement au voyageur organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelTrajet(
            @Parameter(description = "ID du trajet", required = true)
            @PathVariable Long id,
            @RequestParam(required = false) Long requesterId) {
        Voyageur currentVoyageur = currentVoyageurService.currentVoyageur();
        if (requesterId != null && !currentVoyageur.getId().equals(requesterId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        trajetService.cancelTrajet(id, currentVoyageur.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Convertir un trajet privé en public",
            description = "Convertit un trajet PRIVÉ en PUBLIC et applique une restriction à l'organisateur. " +
                    "Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/convert-public")
    public ResponseEntity<Void> convertToPublic(
            @Parameter(description = "ID du trajet", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.checkTrajetAccess(id); // Ligne modifiée
        trajetService.convertToPublic(id);
        return ResponseEntity.noContent().build();
    }
}
