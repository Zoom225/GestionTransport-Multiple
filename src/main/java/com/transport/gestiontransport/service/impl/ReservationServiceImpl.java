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

import java.math.BigDecimal;
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

    @Value("${transport.reservation.delai-paiement:PT30M}")
    private Duration delaiPaiement = Duration.ofMinutes(30);

    @jakarta.annotation.PostConstruct
    void verifierDelaiPaiement() {
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
                && !trajet.getChauffeur().getId().equals(requesterId)) {
            throw new BusinessException("Pour un trajet privé, seul le chauffeur peut inviter des clients.");
        }
        if (trajet.getTypeTrajet() == TypeTrajet.PUBLIC && !voyageurId.equals(requesterId)) {
            throw new BusinessException("Un voyageur ne peut réserver un trajet public que pour lui-même.");
        }
        verifierEligibilite(trajet, voyageur);
        if (reservationRepository.existsByTrajetIdAndVoyageurIdAndStatutIn(trajetId, voyageurId, ACTIVES)) {
            throw new BusinessException("Ce voyageur est déjà inscrit à ce trajet.");
        }
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .trajet(trajet).voyageur(voyageur).statut(StatutReservation.EN_ATTENTE).build());
        Paiement paiement = paiementRepository.save(Paiement.builder().reservation(reservation)
                .montant(TrajetService.PRIX_CLIENT_EUROS.doubleValue()).statut(StatutPaiement.EN_ATTENTE).build());
        reservation.setPaiement(paiement);
        return reservation;
    }

    @Override
    @Transactional
    public List<Reservation> createForInvites(Long trajetId, Long requesterId, List<String> matricules) {
        if (matricules == null || matricules.isEmpty()) return List.of();
        Trajet trajet = verrouillerTrajet(trajetId);
        if (trajet.getTypeTrajet() != TypeTrajet.PRIVE || trajet.getChauffeur() == null
                || !trajet.getChauffeur().getId().equals(requesterId)) {
            throw new BusinessException("Seul le chauffeur peut inviter sur son trajet privé.");
        }
        List<Reservation> result = new ArrayList<>();
        for (String matricule : matricules.stream().filter(java.util.Objects::nonNull)
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList()) {
            Voyageur voyageur = voyageurService.getByMatricule(matricule);
            result.add(create(trajetId, voyageur.getId(), requesterId));
        }
        return result;
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
        verifierEligibilite(trajet, reservation.getVoyageur());
        if (estExpiree(reservation)) throw new BusinessException("Le délai de paiement de cette réservation est expiré.");
        if (BigDecimal.valueOf(paiement.getMontant()).compareTo(TrajetService.PRIX_CLIENT_EUROS) != 0) {
            throw new BusinessException("Ce montant historique nécessite une régularisation ; il ne sera pas modifié automatiquement.");
        }
        return reservation;
    }

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
        verifierEligibilite(trajet, reservation.getVoyageur());
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
        if (!reservation.getVoyageur().getId().equals(voyageurId)
                && !reservation.getTrajet().getChauffeur().getId().equals(voyageurId)) {
            throw new org.springframework.security.access.AccessDeniedException("Seul le client ou le chauffeur peut annuler cette réservation.");
        }
        cancel(reservationId);
    }

    @Override
    @Transactional
    public void cancelTrajet(Long trajetId, Long chauffeurId) {
        Trajet trajet = verrouillerTrajet(trajetId);
        if (trajet.getChauffeur() == null || !trajet.getChauffeur().getId().equals(chauffeurId)) {
            throw new BusinessException("Seul le chauffeur peut annuler son trajet.");
        }
        if (trajet.getStatut() == StatutTrajet.ANNULE) throw new BusinessException("Ce trajet est déjà annulé.");
        for (Reservation reservation : reservationRepository.findByTrajetId(trajetId)) {
            if (ACTIVES.contains(reservation.getStatut())) cancel(reservation.getId());
        }
        trajet.setStatut(StatutTrajet.ANNULE);
        trajetRepository.save(trajet);
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

    private void verifierEligibilite(Trajet trajet, Voyageur voyageur) {
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
        if (voyageurService.hasActiveRestriction(voyageur.getId())) throw new BusinessException("Vous avez une restriction active et ne pouvez pas réserver ce trajet.");
        if (voyageurService.hasOutstandingBalance(voyageur.getId())) throw new BusinessException("Vous avez un solde impayé et ne pouvez pas réserver ce trajet.");
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
