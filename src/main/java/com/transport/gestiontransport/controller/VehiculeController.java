package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.request.VehiculeRequest;
import com.transport.gestiontransport.dto.response.VehiculeResponse;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.mapper.VehiculeMapper;
import com.transport.gestiontransport.service.VehiculeService;
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
@RequestMapping("/api/vehicules")
@RequiredArgsConstructor
@Tag(name = "Vehicules", description = "Endpoints de gestion des vehicules de transport.")
public class VehiculeController {

    private final VehiculeService vehiculeService;
    private final VehiculeMapper vehiculeMapper;
    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(
            summary = "Créer un vehicule",
            description = "Crée un vehicule et le rattache à une agence existant. " +
                    "Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicule créé",
                    content = @Content(schema = @Schema(implementation = VehiculeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Agence introuvable",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<VehiculeResponse> create(@Valid @RequestBody VehiculeRequest request) {
        adminAuthorizationService.checkAgenceAccess(request.getAgenceId());
        Vehicule vehicule = vehiculeMapper.toEntity(request);
        Vehicule saved = vehiculeService.create(vehicule, request.getAgenceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les vehicules",
            description = "Retourne la liste des vehicules. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des vehicules retournée",
                    content = @Content(schema = @Schema(implementation = VehiculeResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<VehiculeResponse>> getAll() {
        List<VehiculeResponse> vehicules = vehiculeService.getAll()
                .stream()
                .map(vehiculeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(vehicules);
    }

    @Operation(
            summary = "Consulter un vehicule",
            description = "Retourne un vehicule par son ID. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicule trouvé",
                    content = @Content(schema = @Schema(implementation = VehiculeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicule introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<VehiculeResponse> getById(
            @Parameter(description = "ID du vehicule", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(vehiculeMapper.toResponse(vehiculeService.getById(id)));
    }

    @Operation(
            summary = "Lister les vehicules d'une agence",
            description = "Retourne les vehicules d'une agence donné. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicules de cette agence retournés",
                    content = @Content(schema = @Schema(implementation = VehiculeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agence introuvable",
                    content = @Content)
    })
    @GetMapping("/agence/{agenceId}")
    public ResponseEntity<List<VehiculeResponse>> getByAgenceId(
            @Parameter(description = "ID de cette agence", required = true)
            @PathVariable Long agenceId) {
        List<VehiculeResponse> vehicules = vehiculeService.getByAgenceId(agenceId)
                .stream()
                .map(vehiculeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(vehicules);
    }

    @Operation(
            summary = "Modifier un vehicule",
            description = "Modifie un vehicule existant. " +
                    "Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicule modifié",
                    content = @Content(schema = @Schema(implementation = VehiculeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Vehicule introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<VehiculeResponse> update(
            @Parameter(description = "ID du vehicule à modifier", required = true)
            @PathVariable Long id,
            @Valid @RequestBody VehiculeRequest request) {
        adminAuthorizationService.checkVehiculeAccess(vehiculeService.getById(id));
        Vehicule vehicule = vehiculeMapper.toEntity(request);
        Vehicule updated = vehiculeService.update(id, vehicule);
        return ResponseEntity.ok(vehiculeMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer un vehicule",
            description = "Supprime définitivement un vehicule et ses trajets associés. " +
                    "Accessible à un admin GLOBAL ou à un admin AGENCE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehicule supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Vehicule introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du vehicule à supprimer", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.checkVehiculeAccess(vehiculeService.getById(id));
        vehiculeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
