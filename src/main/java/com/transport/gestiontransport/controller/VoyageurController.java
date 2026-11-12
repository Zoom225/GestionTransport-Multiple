package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.config.JwtConfig;
import com.transport.gestiontransport.dto.request.VoyageurLoginRequest; // Added import
import com.transport.gestiontransport.dto.request.VoyageurRequest;
import com.transport.gestiontransport.dto.response.VoyageurResponse;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.mapper.VoyageurMapper;
import com.transport.gestiontransport.service.VoyageurService;
import com.transport.gestiontransport.service.AgenceService;
import com.transport.gestiontransport.service.impl.AdminAuthorizationService;
import com.transport.gestiontransport.service.impl.VoyageurCourantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;

@RestController
@RequestMapping("/api/voyageurs")
@RequiredArgsConstructor
@Tag(name = "Voyageurs", description = "Endpoints de gestion des voyageurs de transport. " +
        "Les voyageurs se connectent avec leur matricule via /api/voyageurs/login.")
public class VoyageurController {
    @GetMapping("/{id}/restrictions")
    public ResponseEntity<List<com.transport.gestiontransport.dto.response.RestrictionResponse>> restrictions(@PathVariable Long id) {
        adminAuthorizationService.checkVoyageurAccess(voyageurService.getById(id));
        java.time.LocalDate aujourdHui = java.time.LocalDate.now();
        return ResponseEntity.ok(voyageurService.getRestrictions(id).stream().map(r ->
                new com.transport.gestiontransport.dto.response.RestrictionResponse(r.getId(), r.getMotif(), r.getSourceEvenement(),
                        r.getDateDebut(), r.getDateFin(), r.getDateFin().isAfter(aujourdHui)
                        && (r.getDateDebut() == null || !r.getDateDebut().isAfter(aujourdHui)))).toList());
    }


    private final VoyageurService voyageurService;
    private final AgenceService agenceService;
    private final VoyageurMapper voyageurMapper;
    private final JwtConfig jwtConfig;
    private final AdminAuthorizationService adminAuthorizationService;
    private final VoyageurCourantService currentVoyageurService;
    private final PasswordEncoder passwordEncoder;

    @Operation(
            summary = "Créer un voyageur",
            description = "Crée un voyageur avec un matricule unique. " +
                    "Un voyageur AGENCE doit être rattaché à une agence. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Voyageur créé",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide, format de matricule incorrect ou matricule déjà existant",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Agence introuvable lorsque agenceId est fourni",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<VoyageurResponse> create(@Valid @RequestBody VoyageurRequest request) {
        Voyageur voyageur = voyageurMapper.toEntity(request);

        if (request.getAgenceId() != null) {
            Agence agence = agenceService.getById(request.getAgenceId());
            voyageur.setAgence(agence);
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new com.transport.gestiontransport.exception.BusinessException("Un mot de passe initial personnel est obligatoire.");
        }
        voyageur.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        adminAuthorizationService.checkVoyageurAccess(voyageur);
        Voyageur saved = voyageurService.create(voyageur);
        return ResponseEntity.status(HttpStatus.CREATED).body(voyageurMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les voyageurs",
            description = "Retourne les voyageurs visibles pour l'administrateur connecté.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des voyageurs retournée",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<VoyageurResponse>> getAll() {
        List<VoyageurResponse> voyageurs = adminAuthorizationService.filterVoyageurs(voyageurService.getAll())
                .stream()
                .map(voyageurMapper::toResponse)
                .toList();
        return ResponseEntity.ok(voyageurs);
    }

    @Operation(
            summary = "Consulter un voyageur par ID",
            description = "Retourne un voyageur par son ID interne. " +
                    "Accessible au voyageur propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voyageur trouvé",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<VoyageurResponse> getById(
            @Parameter(description = "ID interne du voyageur", required = true)
            @PathVariable Long id) {
        Voyageur voyageur = voyageurService.getById(id);
        checkCurrentAccess(voyageur);
        return ResponseEntity.ok(voyageurMapper.toResponse(voyageur));
    }

    @Operation(
            summary = "Consulter un voyageur par matricule",
            description = "Retourne un voyageur à partir de son matricule. " +
                    "Accessible aux voyageurs et aux administrateurs authentifiés.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voyageur trouvé",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable avec ce matricule",
                    content = @Content)
    })
    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<VoyageurResponse> getByMatricule(
            @Parameter(description = "Matricule unique du voyageur", required = true)
            @PathVariable String matricule) {
        Voyageur voyageur = voyageurService.getByMatricule(matricule);
        checkCurrentAccess(voyageur);
        return ResponseEntity.ok(voyageurMapper.toResponse(voyageur));
    }

    @Operation(
            summary = "Consulter le voyageur connecté",
            description = "Retourne les informations du voyageur authentifié. " +
                    "Accessible uniquement à un voyageur connecté.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voyageur connecté trouvé",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur connecté introuvable",
                    content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<VoyageurResponse> me() {
        Voyageur voyageur = currentVoyageurService.currentVoyageur();
        return ResponseEntity.ok(voyageurMapper.toResponse(voyageur));
    }

    @Operation(
            summary = "Vérifier la restriction active d'un voyageur",
            description = "Retourne true si le voyageur a une restriction active. " +
                    "Pendant 7 jours, le voyageur ne peut plus créer ni rejoindre de trajet. " +
                    "Accessible au voyageur propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de restriction retourné",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}/restriction")
    public ResponseEntity<Boolean> hasActiveRestriction(
            @Parameter(description = "ID du voyageur à vérifier", required = true)
            @PathVariable Long id) {
        try {
            checkCurrentAccess(voyageurService.getById(id));
            return ResponseEntity.ok(voyageurService.hasActiveRestriction(id));
        } catch (com.transport.gestiontransport.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(
            summary = "Vérifier le solde dû d'un voyageur",
            description = "Retourne true si le voyageur a un solde dû. " +
                    "Un voyageur avec un solde dû ne peut pas créer de nouveau trajet tant que ce solde n'est pas réglé. " +
                    "Le solde est automatiquement ajouté au prochain paiement. " +
                    "Accessible au voyageur propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de solde retourné",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<Boolean> hasOutstandingBalance(
            @Parameter(description = "ID du voyageur à vérifier", required = true)
            @PathVariable Long id) {
        try {
            checkCurrentAccess(voyageurService.getById(id));
            return ResponseEntity.ok(voyageurService.hasOutstandingBalance(id));
        } catch (com.transport.gestiontransport.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(
            summary = "Modifier un voyageur",
            description = "Modifie les informations personnelles d'un voyageur existant. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voyageur modifié",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé : token administrateur requis",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<VoyageurResponse> update(
            @Parameter(description = "ID du voyageur à modifier", required = true)
            @PathVariable Long id,
            @Valid @RequestBody VoyageurRequest request) {
        adminAuthorizationService.checkVoyageurAccess(voyageurService.getById(id));
        Voyageur voyageur = voyageurMapper.toEntity(request);
        Voyageur updated = voyageurService.update(id, voyageur);
        return ResponseEntity.ok(voyageurMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer un voyageur",
            description = "Supprime définitivement un voyageur et ses données associées. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Voyageur supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé : token administrateur requis",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du voyageur à supprimer", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.checkVoyageurAccess(voyageurService.getById(id));
        voyageurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Connexion voyageur",
            description = "Authentifie un voyageur avec son matricule et retourne un JWT pour accéder aux ressources protégées."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie : JWT retourné",
                    content = @Content(schema = @Schema(implementation = VoyageurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable avec ce matricule",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<VoyageurResponse> login(@Valid @RequestBody VoyageurLoginRequest request) { // Modified
        Voyageur voyageur = voyageurService.authenticate(request.getMatricule(), request.getPassword()); // Modified
        VoyageurResponse response = voyageurMapper.toResponse(voyageur);
        String token = jwtConfig.generateToken(voyageur.getMatricule(), voyageur.getTypeVoyageur().name());
        response.setToken(token);
        return ResponseEntity.ok(response);
    }

    private void checkCurrentAccess(Voyageur voyageur) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            if (adminAuthorizationService.currentAdmin().getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
                return;
            }
            adminAuthorizationService.checkVoyageurAccess(voyageur);
            return;
        }

        currentVoyageurService.requireCurrentVoyageur(voyageur.getId());
    }
}
