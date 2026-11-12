package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Paiement;
import com.transport.gestiontransport.entity.Reservation;
import com.transport.gestiontransport.entity.enums.StatutPaiement;
import com.transport.gestiontransport.entity.enums.StatutReservation;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.TrajetRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import com.transport.gestiontransport.repository.PaiementRepository;
import com.transport.gestiontransport.service.PaiementService;
import com.transport.gestiontransport.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementServiceImpl implements PaiementService {

    private final PaiementRepository paiementRepository;
    private final ReservationService reservationService;
    private final VoyageurRepository voyageurRepository;
    private final TrajetRepository trajetRepository;

    @Override
    @Transactional
    public Paiement pay(Long reservationId, Long voyageurId) {
        Reservation reservation = reservationService.getById(reservationId);
        Voyageur voyageur = reservation.getVoyageur();

        // Regle metier : seul le voyageur lié a la reservation peut payer.
        if (!voyageur.getId().equals(voyageurId)) {
            throw new BusinessException("Seul le voyageur associé à cette réservation peut payer.");
        }

        // Regle metier : une reservation annulee ne peut pas etre payee.
        if (reservation.getStatut() == com.transport.gestiontransport.entity.enums.StatutReservation.ANNULEE) {
            throw new BusinessException("Impossible de payer une réservation annulée.");
        }

        Paiement paiement = reservation.getPaiement();
        if (paiement == null) {
            throw new ResourceNotFoundException("Aucun paiement trouvé pour la réservation : " + reservationId);
        }

        // Regle metier : un paiement deja effectue ne peut pas etre refait.
        if (paiement.getStatut() == StatutPaiement.PAYE) {
            throw new BusinessException("Le paiement de cette réservation a déjà été effectué.");
        }

        // Regle metier : un paiement annule ne peut pas etre relance.
        if (paiement.getStatut() == StatutPaiement.ANNULE) {
            throw new BusinessException("Le paiement de cette réservation a été annulé.");
        }
        // Regle metier : la place est deja bloquee par la reservation en attente.
        // Le verrou garde un paiement coherent avec l'etat courant du trajet.
        trajetRepository.findByIdForUpdate(reservation.getTrajet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'ID : " + reservation.getTrajet().getId()));
        // Regle metier : seules les reservations en attente peuvent etre payees.
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seules les réservations en attente peuvent être payées.");
        }
        double montantFinal = paiement.getMontant();
        if (voyageur.getSolde() > 0.0) {
            // Regle metier : un solde impaye est recupere au paiement suivant.
            montantFinal += voyageur.getSolde();
            log.info("Solde impayé de {} ajouté au paiement du voyageur {}",
                    voyageur.getSolde(), voyageurId);
            voyageur.setSolde(0.0);
            voyageurRepository.save(voyageur);
        }

        paiement.setMontant(montantFinal);
        paiement.setStatut(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());
        paiementRepository.save(paiement);

        reservationService.confirm(reservationId);

        log.info("Paiement effectué pour la réservation {} par le voyageur {}", reservationId, voyageurId);

        return paiement;
    }

    @Override
    public Paiement getById(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable avec l'ID : " + id));
    }

    @Override
    public Paiement getByReservationId(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour la réservation : " + reservationId));
    }

    @Override
    public List<Paiement> getByVoyageurId(Long voyageurId) {
        return paiementRepository.findByReservationVoyageurId(voyageurId);
    }

    @Override
    public void checkPaiementOwner(Long paiementId, Long voyageurId) {
        Paiement paiement = getById(paiementId);
        // Regle metier : un voyageur ne peut consulter que ses propres paiements.
        if (!paiement.getReservation().getVoyageur().getId().equals(voyageurId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre voyageur.");
        }
    }

    @Override
    public void checkReservationPaiementOwner(Long reservationId, Long voyageurId) {
        Paiement paiement = getByReservationId(reservationId);
        // Regle metier : un voyageur ne peut agir que sur le paiement de sa reservation.
        if (!paiement.getReservation().getVoyageur().getId().equals(voyageurId)) {
            throw new BusinessException("Accès refusé : ce paiement appartient à un autre voyageur.");
        }
    }

    @Override
    @Transactional
    public void checkUnpaidBeforeTrajet() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Regle metier : a J-1, les reservations non payees sont annulees et imputees a l'organisateur.
        paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)
                .stream()
                .filter(p -> p.getReservation().getTrajet()
                        .getDateDebut().toLocalDate().equals(tomorrow))
                .forEach(p -> {
                    Reservation reservation = p.getReservation();
                    Trajet trajet = reservation.getTrajet();

                    reservationService.cancel(reservation.getId());

                    double partManquante = trajet.getPrixParPassager();
                    Voyageur organisateur = trajet.getChauffeur();
                    organisateur.setSolde(organisateur.getSolde() + partManquante);
                    voyageurRepository.save(organisateur);

                    log.info("Réservation impayée {} annulée, solde {} ajouté à l'organisateur {}",
                            reservation.getId(), partManquante, organisateur.getId());
                });
    }
}
