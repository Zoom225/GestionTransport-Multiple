package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.*;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private static final List<StatutReservation> ACTIVES =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE);
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final TrajetRepository trajetRepository;
    private final VoyageurService voyageurService;
    private final EntityManager entityManager;
    private final AdminAuthorizationService adminAuthorizationService;
    private final VoyageurCourantService voyageurCourantService;
    private final VoyageurEligibilityService eligibilityService;

    @Value("${transport.reservation.delai-paiement:PT30M}")
    private Duration delaiPaiement = Duration.ofMinutes(30);

    @Value("${transport.penalites.annulation-tardive.active:false}") private boolean annulationTardiveActive;
    @Value("${transport.penalites.annulation-tardive.seuil-heures:0}") private int seuilAnnulationHeures;
    @Value("${transport.penalites.absence.active:false}") private boolean absenceActive;
    @Value("${transport.penalites.chauffeur.active:false}") private boolean annulationChauffeurActive;
    @Value("${transport.penalites.chauffeur.seuil-nombre:0}") private int seuilAnnulationsChauffeur;
    @Value("${transport.penalites.chauffeur.fenetre-jours:0}") private int fenetreChauffeurJours;

    @jakarta.annotation.PostConstruct
    void verifierDelaiPaiement() {
        if ((annulationTardiveActive && seuilAnnulationHeures < 1)
                || (annulationChauffeurActive && (seuilAnnulationsChauffeur < 1 || fenetreChauffeurJours < 1))) {
            throw new IllegalStateException("Les seuils des pénalités activées doivent être explicitement configurés et positifs.");
        }
        if (delaiPaiement.isZero() || delaiPaiement.isNegative()) {
            throw new IllegalStateException("Le délai de paiement doit être strictement positif.");
        }
    }

    @Override
    @Transactional
    public Reservation create(Long trajetId, Long voyageurId, Long requesterId) {
        Trajet trajet = verrouillerTrajet(trajetId);
        Voyageur voyageur = voyageurService.getById(voyageurId);
        if (trajet.getChauffeur() == null) throw new BusinessException("Le chauffeur du trajet est obligatoire.");
        if (trajet.getTypeTrajet() == TypeTrajet.PRIVE
                && !peutInviter(trajet, requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException("Seul l’organisateur ou le chauffeur affecté peut inviter.");
        }
        if (trajet.getTypeTrajet() == TypeTrajet.PUBLIC && !voyageurId.equals(requesterId)) {
            throw new BusinessException("Un voyageur ne peut réserver un trajet public que pour lui-même.");
        }
        eligibilityService.verifier(voyageurId, !voyageurId.equals(requesterId));
        verifierConditionsTrajet(trajet, voyageur);
        if (reservationRepository.existsByTrajetIdAndVoyageurIdAndStatutIn(trajetId, voyageurId, ACTIVES)) {
            throw new BusinessException("Ce voyageur est déjà inscrit à ce trajet.");
        }
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .trajet(trajet).voyageur(voyageur).statut(StatutReservation.EN_ATTENTE).build());
        Paiement paiement = paiementRepository.save(Paiement.builder().reservation(reservation)
                .montant(TrajetService.PRIX_CLIENT_EUROS).statut(StatutPaiement.EN_ATTENTE).build());
        reservation.setPaiement(paiement);
        return reservation;
    }

    @Override
    @Transactional
    public List<Reservation> createForInvites(Long trajetId, Long requesterId, List<String> matricules) {
        if (matricules == null || matricules.isEmpty()) return List.of();
        Trajet trajet = verrouillerTrajet(trajetId);
        if (trajet.getTypeTrajet() != TypeTrajet.PRIVE || !peutInviter(trajet, requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException("Seul l’organisateur ou le chauffeur affecté peut inviter sur un trajet privé.");
        }
        List<Long> invites = matricules.stream().filter(java.util.Objects::nonNull)
                .map(String::trim).filter(value -> !value.isEmpty()).distinct()
                .map(voyageurService::getByMatricule).map(Voyageur::getId).distinct().sorted().toList();
        List<Reservation> result = new ArrayList<>();
        for (Long inviteId : invites) {
            result.add(create(trajetId, inviteId, requesterId));
        }
        return result;
    }

    private boolean peutInviter(Trajet trajet, Long auteurId) {
        return trajet.estResponsable(auteurId)
                || (trajet.getChauffeur() != null && trajet.getChauffeur().getId().equals(auteurId));
    }

    @Override
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable avec l'ID : " + id));
    }

    @Override
    public List<Reservation> getByTrajetId(Long trajetId) {
        if (adminAuthorizationService.isAdmin()) {
            adminAuthorizationService.checkTrajetAccess(trajetId);
            return reservationRepository.findByTrajetId(trajetId);
        }
        Voyageur voyageur = voyageurCourantService.currentVoyageur();
        Trajet trajet = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable."));
        return reservationRepository.findByTrajetId(trajetId).stream()
                .filter(r -> trajet.getChauffeur().getId().equals(voyageur.getId())
                        || r.getVoyageur().getId().equals(voyageur.getId())).toList();
    }

    @Override
    public List<Reservation> getByVoyageurId(Long voyageurId) { return reservationRepository.findByVoyageurId(voyageurId); }

    @Override
    public void checkReservationOwner(Long reservationId, Long voyageurId) {
        if (!getById(reservationId).getVoyageur().getId().equals(voyageurId)) {
            throw new BusinessException("Accès refusé : cette réservation appartient à un autre voyageur.");
        }
    }

    /**
     * Règle métier : Préparation et vérification d'éligibilité avant paiement d'une place.
     * Le voyageur ne paie pas le trajet entier en un bloc, mais sa place individuelle (25 €).
     * Une demande EN_ATTENTE ne garantit pas la place tant que le paiement n'est pas validé.
     * Seul le titulaire de la réservation peut procéder au règlement dans le délai imparti.
     */
    @Override
    @Transactional
    public Reservation preparePaiement(Long reservationId, Long voyageurId) {
        Reservation reservation = getById(reservationId);
        Trajet trajet = verrouillerTrajet(reservation.getTrajet().getId());
        entityManager.refresh(reservation);
        if (!reservation.getVoyageur().getId().equals(voyageurId)) {
            throw new BusinessException("Seul le voyageur associé à cette réservation peut payer.");
        }
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seules les réservations en attente peuvent être payées.");
        }
        Paiement paiement = reservation.getPaiement();
        if (paiement == null || paiement.getStatut() != StatutPaiement.EN_ATTENTE) {
            throw new BusinessException("Ce paiement ne peut pas être validé une seconde fois.");
        }
        eligibilityService.verifier(voyageurId, false);
        verifierConditionsTrajet(trajet, reservation.getVoyageur());
        if (estExpiree(reservation)) throw new BusinessException("Le délai de paiement de cette réservation est expiré.");
        if (paiement.getMontant().compareTo(TrajetService.PRIX_CLIENT_EUROS) != 0) {
            throw new BusinessException("Ce montant historique nécessite une régularisation ; il ne sera pas modifié automatiquement.");
        }
        return reservation;
    }

    /**
     * Règle métier : Confirmation de réservation et attribution définitive de la place.
     * Principe « Premier payé, premier servi » :
     * 1. La place n'est confirmée qu'après validation effective du paiement (statut PAYE).
     * 2. L'incrémentation du compteur passagers (nbPassagersActuels) attribue formellement la place.
     * 3. Si les 4 places clients sont confirmées, le trajet passe automatiquement au statut COMPLET.
     * 4. Toute tentative ultérieure de confirmation sur ce trajet sera rejetée.
     */
    @Override
    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = getById(reservationId);
        Trajet trajet = verrouillerTrajet(reservation.getTrajet().getId());
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE
                || reservation.getPaiement() == null || reservation.getPaiement().getStatut() != StatutPaiement.PAYE) {
            throw new BusinessException("Une réservation n'est confirmée qu'après paiement validé.");
        }
        if (estExpiree(reservation)) throw new BusinessException("Le délai de paiement est expiré.");
        eligibilityService.verifier(reservation.getVoyageur().getId(), false);
        verifierConditionsTrajet(trajet, reservation.getVoyageur());
        reservation.setStatut(StatutReservation.CONFIRMEE);
        modifierCompteur(trajet, 1);
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        Trajet trajet = verrouillerTrajet(reservation.getTrajet().getId());
        entityManager.refresh(reservation);
        if (!ACTIVES.contains(reservation.getStatut())) {
            throw new BusinessException("La réservation est déjà annulée ou expirée.");
        }
        if (!trajet.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("L'annulation n'est plus autorisée après le départ.");
        }
        boolean confirmee = reservation.getStatut() == StatutReservation.CONFIRMEE;
        reservation.setStatut(StatutReservation.ANNULEE);
        annulerPaiement(reservation);
        if (confirmee) modifierCompteur(trajet, -1);
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void cancelForUser(Long reservationId, Long voyageurId) {
        Reservation reservation = getById(reservationId);
        verrouillerTrajet(reservation.getTrajet().getId());
        entityManager.refresh(reservation);
        if (!reservation.getVoyageur().getId().equals(voyageurId)
                && !reservation.getTrajet().getChauffeur().getId().equals(voyageurId)) {
            throw new org.springframework.security.access.AccessDeniedException("Seul le client ou le chauffeur peut annuler cette réservation.");
        }
        boolean tardive = annulationTardiveActive
                && reservation.getVoyageur().getId().equals(voyageurId)
                && reservation.getStatut() == StatutReservation.CONFIRMEE
                && reservation.getTrajet().getDateDebut().isAfter(LocalDateTime.now())
                && !reservation.getTrajet().getDateDebut().isAfter(LocalDateTime.now().plusHours(seuilAnnulationHeures));
        cancel(reservationId);
        if (tardive) voyageurService.addRestrictionPourEvenement(voyageurId,
                "annulation-client:" + reservationId, "Annulation tardive d'une réservation confirmée");
    }

    /**
     * Règle métier : Annulation intégrale du trajet.
     * - Autorisation : Seul l'organisateur responsable ou le chauffeur affecté peut annuler le trajet entier.
     * - Horaire : L'annulation est strictement interdite dès l'horaire de départ, même si le trajet est vide.
     * - Réservations : Toutes les réservations actives passent à ANNULEE et les paiements confirmés sont marqués REMBOURSE.
     * - Dettes / pénalités : Aucune dette préexistante n'est supprimée ou modifiée.
     * - Traçabilité : La date et l'identifiant de l'auteur de l'annulation sont enregistrés dans l'entité.
     */
    @Override
    @Transactional
    public void cancelTrajet(Long trajetId, Long chauffeurId) {
        Trajet trajet = verrouillerTrajet(trajetId);
        boolean chauffeurAffecte = trajet.getChauffeur() != null && trajet.getChauffeur().getId().equals(chauffeurId);
        if (!trajet.estResponsable(chauffeurId) && !chauffeurAffecte) {
            throw new BusinessException("Vous n'êtes pas autorisé à annuler ce trajet.");
        }
        if (trajet.getStatut() == StatutTrajet.ANNULE) throw new BusinessException("Ce trajet est déjà annulé.");
        if (!trajet.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("L'annulation n'est plus autorisée dès le départ du trajet.");
        }
        for (Reservation reservation : reservationRepository.findByTrajetId(trajetId)) {
            if (ACTIVES.contains(reservation.getStatut())) cancel(reservation.getId());
        }
        trajet.setStatut(StatutTrajet.ANNULE);
        trajet.setDateAnnulation(LocalDateTime.now());
        trajet.setAnnuleParVoyageurId(chauffeurId);
        trajetRepository.save(trajet);
        if (annulationChauffeurActive && chauffeurAffecte && trajetRepository.countByChauffeurIdAndStatutAndDateAnnulationAfter(
                chauffeurId, StatutTrajet.ANNULE, LocalDateTime.now().minusDays(fenetreChauffeurJours)) >= seuilAnnulationsChauffeur) {
            voyageurService.addRestrictionPourEvenement(chauffeurId, "annulation-chauffeur:" + trajetId,
                    "Seuil validé d'annulations de trajets par le chauffeur atteint");
        }
    }

    @Override
    @Transactional
    public Reservation declarerAbsence(Long reservationId, String motif) {
        if (!adminAuthorizationService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Déclaration réservée aux administrateurs autorisés.");
        }
        if (motif == null || motif.isBlank() || motif.trim().length() > 500) {
            throw new BusinessException("Le motif de l'absence est obligatoire (500 caractères maximum).");
        }
        Reservation reservation = getById(reservationId);
        adminAuthorizationService.checkTrajetAccess(reservation.getTrajet().getId());
        Trajet trajet = verrouillerTrajet(reservation.getTrajet().getId());
        entityManager.refresh(reservation);
        if (trajet.getStatut() == StatutTrajet.ANNULE || trajet.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Une absence exige un départ passé et une réservation confirmée et payée sur un trajet non annulé.");
        }
        if (reservation.getStatut() != StatutReservation.CONFIRMEE || reservation.getPaiement() == null
                || reservation.getPaiement().getStatut() != StatutPaiement.PAYE) {
            throw new BusinessException("Une absence exige un départ passé et une réservation confirmée et payée sur un trajet non annulé.");
        }
        if (reservation.getAbsenceDeclareeLe() != null) return reservation;
        reservation.setAbsenceDeclareeLe(LocalDateTime.now());
        reservation.setMotifAbsence(motif.trim());
        reservation.setAbsenceDeclareePar(adminAuthorizationService.currentAdmin());
        reservationRepository.save(reservation);
        if (absenceActive) voyageurService.addRestrictionPourEvenement(reservation.getVoyageur().getId(),
                "absence:" + reservationId, "Absence injustifiée déclarée par un administrateur autorisé");
        return reservation;
    }

    @Override
    @Transactional
    public void expirePendingReservations() {
        for (Reservation reservation : reservationRepository.findByStatutOrderByTrajetIdAscIdAsc(StatutReservation.EN_ATTENTE)) {
            if (!TrajetService.estQualifiePourCovoiturage(reservation.getTrajet())) continue;
            if (!estExpiree(reservation)) continue;
            verrouillerTrajet(reservation.getTrajet().getId());
            entityManager.refresh(reservation);
            if (reservation.getStatut() == StatutReservation.EN_ATTENTE && estExpiree(reservation)) {
                reservation.setStatut(StatutReservation.EXPIREE);
                annulerPaiement(reservation);
                reservationRepository.save(reservation);
            }
        }
    }

    private Trajet verrouillerTrajet(Long id) {
        Trajet trajet = trajetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'ID : " + id));
        if (entityManager.getLockMode(trajet) != LockModeType.PESSIMISTIC_WRITE) {
            entityManager.lock(trajet, LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(trajet);
        }
        return trajet;
    }

    private void verifierConditionsTrajet(Trajet trajet, Voyageur voyageur) {
        if (trajet.getChauffeur() == null) throw new BusinessException("Le chauffeur du trajet est obligatoire.");
        if (trajet.getChauffeur().getId().equals(voyageur.getId())) {
            throw new BusinessException("Le chauffeur ne réserve aucune des quatre places clients.");
        }
        if (!TrajetService.estQualifiePourCovoiturage(trajet)) {
            throw new BusinessException("Ce trajet historique n'est pas encore qualifié pour le covoiturage.");
        }
        if (trajet.getStatut() == StatutTrajet.ANNULE) throw new BusinessException("Le trajet est annulé.");
        if (trajet.getStatut() == StatutTrajet.COMPLET || trajet.getNbPassagersActuels() >= Trajet.MAX_CLIENTS) {
            throw new BusinessException("Le trajet est déjà complet.");
        }
        if (!trajet.getDateDebut().isAfter(LocalDateTime.now())) throw new BusinessException("Le départ doit être futur.");
        int maximum = switch (voyageur.getTypeVoyageur()) { case VIP -> 21; case LOCAL -> 14; case LIBRE -> 5; };
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), trajet.getDateDebut().toLocalDate());
        if (jours > maximum) throw new BusinessException("Ce profil ne peut réserver plus de " + maximum + " jours à l'avance.");
        Agence agence = trajet.getVehicule().getAgence() != null ? trajet.getVehicule().getAgence() : trajet.getChauffeur().getAgence();
        if (voyageur.getTypeVoyageur() == TypeVoyageur.LOCAL && (voyageur.getAgence() == null || agence == null
                || !voyageur.getAgence().getId().equals(agence.getId()))) {
            throw new BusinessException("Un voyageur LOCAL ne peut réserver que dans son agence autorisée.");
        }
    }

    private boolean estExpiree(Reservation reservation) {
        return reservation.getCreatedAt() != null
                && !reservation.getCreatedAt().plus(delaiPaiement).isAfter(LocalDateTime.now());
    }

    private void modifierCompteur(Trajet trajet, int delta) {
        int nouveau = trajet.getNbPassagersActuels() + delta;
        if (nouveau < 0 || nouveau > Trajet.MAX_CLIENTS) throw new BusinessException("Capacité clients incohérente.");
        trajet.setNbPassagersActuels(nouveau);
        trajet.setStatut(nouveau == Trajet.MAX_CLIENTS ? StatutTrajet.COMPLET : StatutTrajet.PLANIFIE);
        trajetRepository.save(trajet);
    }

    private void annulerPaiement(Reservation reservation) {
        Paiement paiement = reservation.getPaiement();
        if (paiement == null) return;
        if (paiement.getStatut() == StatutPaiement.PAYE) paiement.setStatut(StatutPaiement.REMBOURSE);
        else if (paiement.getStatut() == StatutPaiement.EN_ATTENTE) paiement.setStatut(StatutPaiement.ANNULE);
        paiementRepository.save(paiement);
    }
}
