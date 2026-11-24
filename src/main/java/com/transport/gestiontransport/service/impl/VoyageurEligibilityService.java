package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Restriction;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.exception.*;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.TrajetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;

@Service
@RequiredArgsConstructor
public class VoyageurEligibilityService {
    private final VoyageurRepository voyageurs;
    private final RestrictionRepository restrictions;
    private final TrajetRepository trajets;
    private final Clock clock;

    public record Statut(boolean peutParticiper, boolean suspensionActive, Instant finSuspension,
                         BigDecimal soldeDu, String code, String message, Instant debutSuspension,
                         boolean detteBloquante, boolean peutCreerMatch, boolean peutRejoindreMatch,
                         String motifRestriction) {}
    public record Sanction(Long id, Long trajetId, String motif, Instant debut, Instant fin,
                           BigDecimal montant, boolean payee, boolean active, LocalDateTime dateReglement) {}

    public boolean estActive(Restriction r) {
        Instant maintenant = clock.instant();
        if (r.getFinSuspension() != null) return r.getDebutSuspension() != null
                && !r.getDebutSuspension().isAfter(maintenant) && r.getFinSuspension().isAfter(maintenant);
        LocalDate jour = LocalDate.now(clock);
        return r.getDateFin().isAfter(jour) && (r.getDateDebut() == null || !r.getDateDebut().isAfter(jour));
    }

    @Transactional(readOnly = true)
    public java.util.List<Sanction> historique(Long id) {
        var historique = new java.util.ArrayList<Sanction>();
        var trajetsAvecSanction = new java.util.HashSet<Long>();
        for (Restriction restriction : restrictions.findByVoyageurIdOrderByDateFinDescIdDesc(id)) {
            Trajet trajet = restriction.getTrajetOrigine();
            Long trajetId = trajet == null ? null : trajet.getId();
            LocalDateTime reglement = trajet == null ? null : trajet.getPenaliteIncompletPayeeLe();
            if (trajetId != null) trajetsAvecSanction.add(trajetId);
            historique.add(new Sanction(restriction.getId(), trajetId, restriction.getMotif(),
                    restriction.getDebutSuspension(), restriction.getFinSuspension(), restriction.getMontantPenalite(),
                    reglement != null, estActive(restriction), reglement));
        }
        // Historical penalties have a trip marker but no restriction linked to the trip.
        for (Trajet trajet : trajets.findByOrganisateurIdAndPenaliteIncompletImputeeLeIsNotNullOrderByPenaliteIncompletImputeeLeDesc(id)) {
            if (trajetsAvecSanction.contains(trajet.getId())) continue;
            LocalDateTime reglement = trajet.getPenaliteIncompletPayeeLe();
            historique.add(new Sanction(-trajet.getId(), trajet.getId(), "Pénalité historique pour trajet privé incomplet",
                    null, null, TrajetService.PRIX_TRAJET_EUROS, reglement != null, false, reglement));
        }
        return historique;
    }

    @Transactional(readOnly = true)
    public Statut statut(Long id) {
        return evaluer(voyageurs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable.")));
    }

    // Same voyageur lock as penalty creation and debt settlement. Recheck inside the transaction.
    @Transactional
    public void verifier(Long id, boolean ajoutParTiers) {
        Voyageur voyageur = voyageurs.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur introuvable."));
        Statut etat = evaluer(voyageur);
        if (!etat.peutParticiper()) {
            if (ajoutParTiers) throw new ParticipationException("VOYAGEUR_NON_ELIGIBLE",
                    "Ce voyageur ne peut pas être ajouté au trajet car il est temporairement soumis à une restriction de participation.");
            throw new ParticipationException(etat.code(), etat.message());
        }
    }

    private Statut evaluer(Voyageur voyageur) {
        Instant maintenant = clock.instant();
        Instant fin = restrictions.finSuspensionActive(voyageur.getId(), maintenant);
        boolean ancienneRestriction = restrictions.existsByVoyageurIdAndDateFinAfter(voyageur.getId(), LocalDate.now(clock));
        // A scalar query sees the current balance without cascading REFRESH into pending payments.
        BigDecimal dette = voyageurs.soldeCourant(voyageur.getId());
        boolean impaye = dette.signum() > 0;
        Restriction derniere = restrictions
                .findFirstByVoyageurIdAndDebutSuspensionLessThanEqualOrderByFinSuspensionDescIdDesc(voyageur.getId(), maintenant)
                .orElse(null);
        Instant debut = derniere == null ? null : derniere.getDebutSuspension();
        String motif = derniere == null ? null : derniere.getMotif();
        if (fin != null) {
            String message = impaye
                    ? "Votre compte est suspendu jusqu'au " + afficher(fin)
                            + " à la suite d'un trajet privé incomplet. Vous ne pouvez actuellement ni créer ni rejoindre un trajet."
                    : "Votre pénalité de 100 € est réglée. Votre suspension reste toutefois active jusqu'au " + afficher(fin) + ".";
            return new Statut(false, true, fin, dette, "VOYAGEUR_SUSPENDU", message, debut, impaye, false, false, motif);
        }
        if (ancienneRestriction) return new Statut(false, true, null, dette, "VOYAGEUR_SUSPENDU",
                "Vous avez une restriction active et ne pouvez pas créer ou rejoindre un trajet.",
                null, impaye, false, false, "Restriction de participation");
        Instant derniereFin = derniere == null ? null : derniere.getFinSuspension();
        if (impaye) return new Statut(false, false, derniereFin, dette, "PENALITE_IMPAYEE",
                derniere == null ? "Vous avez un solde impayé. Réglez-le avant de pouvoir participer."
                        : "Votre suspension est terminée. Vous devez régler votre solde dû avant de pouvoir participer à nouveau.",
                debut, true, false, false, motif);
        return new Statut(true, false, derniereFin, dette, null,
                derniere == null ? "Vous pouvez participer aux trajets."
                        : "Votre suspension est terminée et votre pénalité est réglée. Vous pouvez à nouveau créer et rejoindre des trajets.",
                debut, false, Boolean.TRUE.equals(voyageur.getPeutProposerTrajet()), true, null);
    }

    private String afficher(Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss z", java.util.Locale.FRENCH)
                .withZone(ZoneId.of("Europe/Brussels")).format(instant);
    }
}
