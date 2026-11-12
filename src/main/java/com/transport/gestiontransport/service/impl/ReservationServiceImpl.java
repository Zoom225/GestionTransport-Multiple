package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Paiement;
import com.transport.gestiontransport.entity.Reservation;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.PaiementRepository;
import com.transport.gestiontransport.repository.ReservationRepository;
import com.transport.gestiontransport.service.TrajetService;
import com.transport.gestiontransport.service.VoyageurService;
import com.transport.gestiontransport.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final TrajetService trajetService;
    private final VoyageurService voyageurService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Reservation create(Long trajetId, Long voyageurId, Long requesterId) {
        Trajet trajet = trajetService.getTrajetEntityById(trajetId);
        // Serialize reservations on this trajet and reload any state read earlier
        // in the transaction before checking capacity or duplicate voyageurship.
        entityManager.lock(trajet, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(trajet);
        Voyageur voyageur = voyageurService.getById(voyageurId);

        // Regle metier : impossible de reserver un trajet deja complet.
        if (trajet.getStatut() == StatutTrajet.COMPLET) {
            throw new BusinessException("Le trajet est déjà complet.");
        }
        // Regle metier : impossible de reserver un trajet annule.
        if (trajet.getStatut() == StatutTrajet.ANNULE) {
            throw new BusinessException("Le trajet est annulé.");
        }

        // Regle metier : un voyageur ne peut pas etre inscrit deux fois au meme trajet.
        if (reservationRepository.existsByTrajetIdAndVoyageurId(trajetId, voyageurId)) {
            throw new BusinessException("Ce voyageur est déjà inscrit à ce trajet.");
        }

        // Regle metier : un voyageur restreint ne peut pas reserver.
        if (voyageurService.hasActiveRestriction(voyageurId)) {
            throw new BusinessException("Vous avez une restriction active et ne pouvez pas réserver ce trajet.");
        }

        // Regle metier : un voyageur avec un solde impaye ne peut pas reserver.
        if (voyageurService.hasOutstandingBalance(voyageurId)) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas réserver ce trajet.");
        }

        // Le requesterId est l'ID de la personne qui initie la demande de reservation.
        // Regle metier : seul l'organisateur peut inviter des passagers sur un trajet prive.
        if (trajet.getTypeTrajet() == TypeTrajet.PRIVE
                && !trajet.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Pour un trajet privé, seul l'organisateur peut ajouter des passagers.");
        }

        // Regle metier : un voyageur reserve un trajet public uniquement pour lui-meme.
        if (trajet.getTypeTrajet() == TypeTrajet.PUBLIC && !voyageurId.equals(requesterId)) {
            throw new BusinessException("Un voyageur ne peut réserver un trajet public que pour lui-même.");
        }

        if (voyageur.getTypeVoyageur() == TypeVoyageur.AGENCE) {
            Long voyageurAgenceId = voyageur.getAgence().getId();
            Long trajetAgenceId = trajet.getVehicule().getAgence().getId();
            // Regle metier : un voyageur AGENCE ne reserve que sur son agence.
            if (!voyageurAgenceId.equals(trajetAgenceId)) {
                throw new BusinessException("Un voyageur AGENCE ne peut réserver que sur sa propre agence.");
            }
        }

        Reservation reservation = Reservation.builder()
                .trajet(trajet)
                .voyageur(voyageur)
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        reservation = reservationRepository.save(reservation);

        // Regle metier : chaque reservation cree automatiquement un paiement en attente.
        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(trajet.getPrixParPassager())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);
        // Regle metier : une reservation en attente bloque temporairement une place.
        trajetService.incrementPassagers(trajetId);

        log.info("Réservation créée pour le voyageur {} sur le trajet {}", voyageurId, trajetId);

        return reservation;
    }

    @Override
    @Transactional
    public List<Reservation> createForInvites(Long trajetId, Long requesterId, List<String> inviteeMatricules) {
        if (inviteeMatricules == null || inviteeMatricules.isEmpty()) {
            return List.of();
        }

        List<Reservation> createdReservations = new ArrayList<>();
        for (String matricule : inviteeMatricules) {
            String normalizedMatricule = matricule == null ? "" : matricule.trim();
            if (normalizedMatricule.isEmpty()) {
                continue;
            }

            Voyageur voyageur = voyageurService.getByMatricule(normalizedMatricule);
            createdReservations.add(create(trajetId, voyageur.getId(), requesterId));
        }

        return createdReservations;
    }

    @Override
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable avec l'ID : " + id));
    }

    @Override
    public List<Reservation> getByTrajetId(Long trajetId) {
        return reservationRepository.findByTrajetId(trajetId);
    }

    @Override
    public List<Reservation> getByVoyageurId(Long voyageurId) {
        return reservationRepository.findByVoyageurId(voyageurId);
    }

    @Override
    public void checkReservationOwner(Long reservationId, Long voyageurId) {
        Reservation reservation = getById(reservationId);
        // Regle metier : un voyageur ne peut consulter que ses propres reservations.
        if (!reservation.getVoyageur().getId().equals(voyageurId)) {
            throw new BusinessException("Accès refusé : cette réservation appartient à un autre voyageur.");
        }
    }

    @Override
    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        boolean occupiedPlace = reservation.getStatut() != StatutReservation.ANNULEE;

        // Regle metier : impossible d'annuler deux fois la meme reservation.
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("La réservation est déjà annulée.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        reservationRepository.save(reservation);

        // Regle metier : annuler une reservation met aussi a jour son paiement.
        Paiement paiement = reservation.getPaiement();
        if (paiement != null) {
            if (paiement.getStatut() == StatutPaiement.PAYE) {
                paiement.setStatut(StatutPaiement.REMBOURSE);
            } else if (paiement.getStatut() == StatutPaiement.EN_ATTENTE) {
                paiement.setStatut(StatutPaiement.ANNULE);
            }
            paiementRepository.save(paiement);
        }

        // Regle metier : annuler une reservation.
        if (occupiedPlace
                && reservation.getTrajet().getNbPassagersActuels() != null
                && reservation.getTrajet().getNbPassagersActuels() > 0) {
            trajetService.decrementPassagers(reservation.getTrajet().getId());
        }

        log.info("Réservation {} annulée", reservationId);
    }

    @Override
    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = getById(reservationId);
        // Regle metier : impossible de confirmer deux fois la meme reservation.
        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            throw new BusinessException("La réservation est déjà confirmée.");
        }

        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservationRepository.save(reservation);

        log.info("Réservation {} confirmée", reservationId);
    }
}
