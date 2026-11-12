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

    @Override
    @Transactional
    public Paiement pay(Long reservationId, Long voyageurId) {
        Reservation reservation = reservationService.preparePaiement(reservationId, voyageurId);
        Paiement paiement = reservation.getPaiement();
        paiement.setStatut(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());
        paiementRepository.save(paiement);
        reservationService.confirm(reservationId);
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
        reservationService.expirePendingReservations();
    }
}
