package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.request.AgenceRequest;
import com.transport.gestiontransport.dto.response.AgenceResponse;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.mapper.AgenceMapper;
import com.transport.gestiontransport.service.AgenceService;
import com.transport.gestiontransport.service.impl.AdminAuthorizationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agences")
@RequiredArgsConstructor
@Tag(name = "Agences", description = "Endpoints de gestion des agences de transport.")
public class AgenceController {

    private final AgenceService agenceService;
    private final AgenceMapper agenceMapper;
    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(
            summary = "Créer une agence",
            description = "Crée un nouvelcette agence de transport avec ses horaires, sa durée de trajet et sa durée de pause. " +
                    "Accessible uniquement à un admin GLOBAL.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agence créé",
                    content = @Content(schema = @Schema(implementation = AgenceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<AgenceResponse> create(@Valid @RequestBody AgenceRequest request) {
        adminAuthorizationService.requireGlobalAdmin();
        Agence agence = agenceMapper.toEntity(request);
        Agence saved = agenceService.create(agence);
        return ResponseEntity.status(HttpStatus.CREATED).body(agenceMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les agences",
            description = "Retourne la liste des agences de transport. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des agences retournée",
                    content = @Content(schema = @Schema(implementation = AgenceResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<AgenceResponse>> getAll() {
        List<AgenceResponse> agences = agenceService.getAll()
                .stream()
                .map(agenceMapper::toResponse)
                .toList();
        return ResponseEntity.ok(agences);
    }

    @Operation(
            summary = "Consulter une agence",
            description = "Retourne une agence par son ID. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agence trouvé",
                    content = @Content(schema = @Schema(implementation = AgenceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agence introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<AgenceResponse> getById(
            @Parameter(description = "ID de cette agence", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(agenceMapper.toResponse(agenceService.getById(id)));
    }

    @Operation(
            summary = "Modifier une agence",
            description = "Modifie une agence existant. Accessible uniquement à un admin GLOBAL.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agence modifié",
                    content = @Content(schema = @Schema(implementation = AgenceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Agence introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<AgenceResponse> update(
            @Parameter(description = "ID de cette agence à modifier", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AgenceRequest request) {
        adminAuthorizationService.requireGlobalAdmin();
        Agence agence = agenceMapper.toEntity(request);
        Agence updated = agenceService.update(id, agence);
        return ResponseEntity.ok(agenceMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer une agence",
            description = "Supprime définitivement une agence et ses données associées. Accessible uniquement à un admin GLOBAL.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agence supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Agence introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de cette agence à supprimer", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.requireGlobalAdmin();
        agenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
