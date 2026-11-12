package com.transport.gestiontransport.controller;

import com.transport.gestiontransport.dto.request.ReservationInviteRequest;
import com.transport.gestiontransport.dto.request.ReservationRequest;
import com.transport.gestiontransport.dto.response.ReservationResponse;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Reservation;
import com.transport.gestiontransport.mapper.ReservationMapper;
import com.transport.gestiontransport.service.ReservationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints de gestion des réservations de trajet. " +
        "Une réservation relie un voyageur à un trajet et génère automatiquement un paiement en attente. " +
        "La place n'est confirmée qu'après paiement : premier payé, premier servi.")
public class ReservationController {
    public record DeclarationAbsence(@jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 500) String motif) {}

    @Operation(summary = "Déclarer explicitement une absence injustifiée", security = @SecurityRequirement(name = "Bearer Auth"))
    @PostMapping("/{id}/absence")
    public ResponseEntity<ReservationResponse> declarerAbsence(@PathVariable Long id,
            @Valid @RequestBody DeclarationAbsence request) {
        return ResponseEntity.ok(reservationMapper.toResponse(reservationService.declarerAbsence(id, request.motif())));
    }

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;
    private final VoyageurCourantService currentVoyageurService;

    @Operation(
            summary = "Créer une réservation",
            description = "Crée une réservation pour le voyageur connecté sur un trajet donné. " +
                    "Un paiement EN_ATTENTE est créé automatiquement. " +
                    "Pour un trajet PUBLIC, le voyageur ne peut réserver que pour lui-même. " +
                    "Pour un trajet PRIVÉ, seul l'organisateur peut ajouter des passagers. " +
                    "Accessible uniquement à un voyageur authentifié.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée avec un paiement en attente",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée : trajet complet, voyageur déjà inscrit, restriction, solde dû, mauvais agence ou restriction de trajet privé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Trajet ou voyageur introuvable",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        Voyageur currentVoyageur = currentVoyageurService.currentVoyageur();
        Reservation reservation = reservationService.create(
                request.getTrajetId(),
                request.getVoyageurId(),
                currentVoyageur.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationMapper.toResponse(reservation));
    }

    @Operation(
            summary = "Ajouter des passagers à un trajet privé",
            description = "Crée plusieurs réservations à partir de matricules. " +
                    "Le voyageur connecté doit être l'organisateur du trajet privé. " +
                    "Aucune fiche complète d'un autre voyageur n'est exposée au front.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservations créées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée ou matricule introuvable",
                    content = @Content)
    })
    @PostMapping("/private-invites")
    public ResponseEntity<List<ReservationResponse>> createPrivateInvites(
            @Valid @RequestBody ReservationInviteRequest request) {
        Voyageur currentVoyageur = currentVoyageurService.currentVoyageur();
        List<ReservationResponse> reservations = reservationService
                .createForInvites(request.getTrajetId(), currentVoyageur.getId(), request.getInviteeMatricules())
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservations);
    }

    @Operation(
            summary = "Consulter une réservation",
            description = "Retourne une réservation par son ID. " +
                    "Accessible uniquement au voyageur propriétaire de la réservation.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @Parameter(description = "ID de la réservation", required = true)
            @PathVariable Long id) {
        Reservation reservation = reservationService.getById(id);
        currentVoyageurService.requireCurrentVoyageur(reservation.getVoyageur().getId());
        return ResponseEntity.ok(reservationMapper.toResponse(reservation));
    }

    @Operation(
            summary = "Lister les réservations d'un trajet",
            description = "Retourne les réservations liées à un trajet. " +
                    "Accessible aux voyageurs et aux administrateurs selon la configuration de sécurité.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations du trajet retournées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable",
                    content = @Content)
    })
    @GetMapping("/trajet/{trajetId}")
    public ResponseEntity<List<ReservationResponse>> getByTrajetId(
            @Parameter(description = "ID du trajet", required = true)
            @PathVariable Long trajetId) {
        List<ReservationResponse> reservations = reservationService.getByTrajetId(trajetId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Lister les réservations d'un voyageur",
            description = "Retourne les réservations du voyageur indiqué. " +
                    "Accessible uniquement au voyageur connecté correspondant à cet ID.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations du voyageur retournées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Voyageur introuvable",
                    content = @Content)
    })
    @GetMapping("/voyageur/{voyageurId}")
    public ResponseEntity<List<ReservationResponse>> getByVoyageurId(
            @Parameter(description = "ID du voyageur", required = true)
            @PathVariable Long voyageurId) {
        currentVoyageurService.requireCurrentVoyageur(voyageurId);
        List<ReservationResponse> reservations = reservationService.getByVoyageurId(voyageurId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Annuler une réservation",
            description = "Annule une réservation existante. " +
                    "Si le paiement était PAYE, il passe à REMBOURSE. " +
                    "Si la réservation était seulement EN_ATTENTE, le nombre de passagers confirmés n'est pas décrémenté. " +
                    "Accessible uniquement au voyageur propriétaire de la réservation.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation annulée"),
            @ApiResponse(responseCode = "400", description = "Réservation déjà annulée",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable",
                    content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "ID de la réservation à annuler", required = true)
            @PathVariable Long id) {
        reservationService.cancelForUser(id, currentVoyageurService.currentVoyageurId());
        return ResponseEntity.noContent().build();
    }
}
