package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.response.PaiementResponse;
import com.transport.gestiontransport.mapper.PaiementMapper;
import com.transport.gestiontransport.service.PaiementService;
import com.transport.gestiontransport.service.impl.VoyageurCourantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Endpoints de gestion des paiements de réservation. " +
        "Un paiement est créé automatiquement en EN_ATTENTE avec chaque réservation. " +
        "La réservation devient CONFIRMEE uniquement après paiement.")
public class PaiementController {

    private final PaiementService paiementService;
    private final PaiementMapper paiementMapper;
    private final VoyageurCourantService currentVoyageurService;

    @Operation(
            summary = "Payer une réservation pour un voyageur donné",
            description = "Traite le paiement d'une réservation. " +
                    "Le voyageur connecté doit correspondre à l'ID fourni et être propriétaire de la réservation. " +
                    "Endpoint conservé pour compatibilité ; préférer /api/paiements/reservation/{reservationId}/pay.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement effectué et réservation confirmée",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paiement déjà effectué ou réservation non payable",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Réservation ou paiement introuvable",
                    content = @Content)
    })
    @PostMapping("/reservation/{reservationId}/voyageur/{voyageurId}")
    public ResponseEntity<PaiementResponse> pay(
            @Parameter(description = "ID de la réservation à payer", required = true)
            @PathVariable Long reservationId,
            @Parameter(description = "ID du voyageur payeur, qui doit correspondre au voyageur connecté", required = true)
            @PathVariable Long voyageurId) {
        currentVoyageurService.requireCurrentVoyageur(voyageurId);
        return ResponseEntity.ok(
                paiementMapper.toResponse(paiementService.pay(reservationId, voyageurId))
        );
    }

    @Operation(
            summary = "Payer une réservation du voyageur connecté",
            description = "Permet au voyageur connecté de payer une réservation qui lui appartient. " +
                    "La place est confirmée uniquement après ce paiement.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement effectué et réservation confirmée",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paiement déjà effectué ou réservation non payable",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Réservation ou paiement introuvable",
                    content = @Content)
    })
    @PostMapping("/reservation/{reservationId}/pay")
    public ResponseEntity<PaiementResponse> payCurrentVoyageur(
            @Parameter(description = "ID de la réservation à payer", required = true)
            @PathVariable Long reservationId) {
        Long voyageurId = currentVoyageurService.currentVoyageurId();
        return ResponseEntity.ok(
                paiementMapper.toResponse(paiementService.pay(reservationId, voyageurId))
        );
    }

    @Operation(
            summary = "Consulter un paiement",
            description = "Retourne un paiement par son ID. " +
                    "Accessible uniquement au voyageur propriétaire de la réservation associée.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement trouvé",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Paiement introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaiementResponse> getById(
            @Parameter(description = "ID du paiement", required = true)
            @PathVariable Long id) {
        paiementService.checkPaiementOwner(id, currentVoyageurService.currentVoyageurId());
        return ResponseEntity.ok(paiementMapper.toResponse(paiementService.getById(id)));
    }

    @Operation(
            summary = "Consulter le paiement d'une réservation",
            description = "Retourne le paiement associé à une réservation. " +
                    "Accessible uniquement au voyageur propriétaire de cette réservation.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement trouvé",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Paiement introuvable pour cette réservation",
                    content = @Content)
    })
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaiementResponse> getByReservationId(
            @Parameter(description = "ID de la réservation", required = true)
            @PathVariable Long reservationId) {
        paiementService.checkReservationPaiementOwner(reservationId, currentVoyageurService.currentVoyageurId());
        return ResponseEntity.ok(
                paiementMapper.toResponse(paiementService.getByReservationId(reservationId))
        );
    }

    @Operation(
            summary = "Lister les paiements d'un voyageur",
            description = "Retourne l'historique des paiements du voyageur indiqué. " +
                    "Accessible uniquement au voyageur connecté correspondant à cet ID.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiements du voyageur retournés",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @GetMapping("/voyageur/{voyageurId}")
    public ResponseEntity<List<PaiementResponse>> getByVoyageurId(
            @Parameter(description = "ID du voyageur", required = true)
            @PathVariable Long voyageurId) {
        currentVoyageurService.requireCurrentVoyageur(voyageurId);
        List<PaiementResponse> paiements = paiementService.getByVoyageurId(voyageurId)
                .stream()
                .map(paiementMapper::toResponse)
                .toList();
        return ResponseEntity.ok(paiements);
    }
}
