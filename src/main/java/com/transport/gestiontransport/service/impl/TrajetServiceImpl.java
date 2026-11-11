package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.dto.request.TrajetRequest;
import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.mapper.TrajetMapper;
import com.transport.gestiontransport.dto.request.CreateTrajetRequest;
import com.transport.gestiontransport.dto.response.TrajetDto;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.TrajetService;
import com.transport.gestiontransport.service.VoyageurService;
import com.transport.gestiontransport.service.VehiculeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final VoyageurRepository voyageurRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final IndisponibiliteServiceRepository indisponibiliteServiceRepository;
    private final VehiculeService vehiculeService;
    private final VoyageurService voyageurService;
    private final TrajetMapper trajetMapper;

    private static final double TRAJET_PRICE = 60.0;

    @Override
    @Transactional
    public TrajetDto createTrajet(CreateTrajetRequest request, String username) {
        Voyageur organisateur = voyageurRepository.findByMatriculeIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur non trouvé pour l'utilisateur: " + username));

        Vehicule vehicule = vehiculeService.getById(request.vehiculeId());

        // Regle metier : un voyageur avec un solde impaye ne peut pas creer de trajet.
        if (voyageurService.hasOutstandingBalance(organisateur.getId())) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas créer de trajet.");
        }
        // Regle metier : un voyageur restreint ne peut pas creer de trajet.
        if (voyageurService.hasActiveRestriction(organisateur.getId())) {
            throw new BusinessException("Vous avez une restriction active et ne pouvez pas créer de trajet.");
        }

        validateBookingDelay(organisateur, request.trajetDate().toLocalDate());
        validateAgenceAccessForOrganizer(organisateur, vehicule);
        //Service metier debut et fin trajet
        LocalDateTime dateDebut = request.trajetDate();
        LocalDateTime dateFin = dateDebut.plusMinutes(vehicule.getAgence().getDureeTrajetMinutes());

        validateAgenceNotClosed(vehicule, dateDebut.toLocalDate());
        validateAgenceOpeningHours(vehicule, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : un vehicule ne peut pas avoir deux trajets sur le meme creneau horaire.
        if (!isSlotAvailable(vehicule, dateDebut, dateFin)) {
            throw new BusinessException("Ce créneau est déjà réservé sur le vehicule : " + vehicule.getId());
        }

        Trajet trajet = new Trajet();
        trajet.setOrganisateur(organisateur);
        trajet.setVehicule(vehicule);
        trajet.setDateDebut(dateDebut);
        trajet.setDateFin(dateFin);
        trajet.setTypeTrajet(TypeTrajet.valueOf(request.trajetType()));
        trajet.setStatut(StatutTrajet.PLANIFIE);
        trajet.setNbPassagersActuels(0);
        trajet.setPrixTotal(TRAJET_PRICE);
        trajet.setPrixParPassager(TRAJET_PRICE / REQUIRED_PASSAGERS_PER_TRAJET);

        Trajet savedTrajet = trajetRepository.save(trajet);
        // Regle metier : l'organisateur commence avec une reservation en attente et un paiement associe.
        createPendingReservationForOrganizer(savedTrajet, organisateur);
        log.info("Trajet créé avec succès (ID: {}) par l'utilisateur {}", savedTrajet.getId(), username);

        return trajetMapper.toTrajetDto(savedTrajet);
    }

    @Override
    public List<TrajetDto> findAllTrajets() {
        return trajetRepository.findAll().stream()
                .map(trajetMapper::toTrajetDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrajetDto> getPublicAvailableTrajets() {
        return trajetRepository.findByTypeTrajetAndStatut(TypeTrajet.PUBLIC, StatutTrajet.PLANIFIE)
                .stream()
                .map(trajetMapper::toTrajetDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrajetDto> findByOrganisateur(Long organisateurId) {
        return trajetRepository.findByOrganisateurId(organisateurId)
                .stream()
                .map(trajetMapper::toTrajetDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrajetDto> findByAgence(Long agenceId) {
        return trajetRepository.findByVehiculeAgenceId(agenceId)
                .stream()
                .map(trajetMapper::toTrajetDto)
                .collect(Collectors.toList());
    }

    @Override
    public Trajet getTrajetEntityById(Long id) {
        return trajetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet non trouvé avec l'ID : " + id));
    }

    @Override
    public TrajetDto getTrajetDtoById(Long id) {
        return trajetMapper.toTrajetDto(getTrajetEntityById(id));
    }

    @Override
    @Transactional
    public TrajetDto updateTrajet(Long trajetId, TrajetRequest request) {
        Trajet trajet = getTrajetEntityById(trajetId);

        // Regle metier : un trajet annule ne peut plus etre modifie.
        if (trajet.getStatut() == StatutTrajet.ANNULE) {
            throw new BusinessException("Impossible de modifier un trajet annulé.");
        }
        // Regle metier : seul l'organisateur peut modifier son trajet.
        if (!trajet.getOrganisateur().getId().equals(request.getOrganisateurId())) {
            throw new BusinessException("Seul l'organisateur peut modifier ce trajet.");
        }

        Vehicule vehicule = vehiculeService.getById(request.getVehiculeId());
        LocalDateTime dateDebut = request.getDate().atTime(request.getHeureDebut());
        LocalDateTime dateFin = dateDebut.plusMinutes(vehicule.getAgence().getDureeTrajetMinutes());

        validateAgenceNotClosed(vehicule, dateDebut.toLocalDate());
        validateAgenceOpeningHours(vehicule, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : la modification ne doit pas creer de chevauchement de creneau.
        if (!isSlotAvailableExcluding(vehicule, dateDebut, dateFin, trajetId)) {
            throw new BusinessException("Ce créneau est déjà réservé sur le vehicule : " + vehicule.getId());
        }

        trajet.setVehicule(vehicule);
        trajet.setDateDebut(dateDebut);
        trajet.setDateFin(dateFin);
        trajet.setTypeTrajet(request.getTypeTrajet());

        return trajetMapper.toTrajetDto(trajetRepository.save(trajet));
    }

    @Override
    @Transactional
    public void cancelTrajet(Long trajetId, Long requesterId) {
        Trajet trajet = getTrajetEntityById(trajetId);

        // Regle metier : un trajet deja annule ne peut pas etre annule une seconde fois.
        if (trajet.getStatut() == StatutTrajet.ANNULE) {
            throw new BusinessException("Ce trajet est déjà annulé.");
        }
        // Regle metier : seul l'organisateur peut annuler son trajet.
        if (!trajet.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut annuler ce trajet.");
        }

        trajet.setStatut(StatutTrajet.ANNULE);
        trajetRepository.save(trajet);
        log.info("Trajet {} annulé par l'organisateur {}", trajetId, requesterId);
    }

    @Override
    @Transactional
    public void checkAndConvertExpiredPrivateTrajets() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfDay = tomorrow.atStartOfDay();
        LocalDateTime endOfDay = tomorrow.atTime(LocalTime.MAX);

        // Regle metier : a J-1, un trajet prive incomplet redevient public et restreint son organisateur.
        List<Trajet> expiredTrajets = trajetRepository
                .findByDateDebutBetweenAndStatut(startOfDay, endOfDay, StatutTrajet.PLANIFIE)
                .stream()
                .filter(m -> m.getTypeTrajet() == TypeTrajet.PRIVE && m.getNbPassagersActuels() < REQUIRED_PASSAGERS_PER_TRAJET)
                .toList();
        //sheduler active converti en public la veille
        if (!expiredTrajets.isEmpty()) {
            expiredTrajets.forEach(m -> convertToPublic(m.getId()));
            log.info("Scheduler : {} trajet(s) privé(s) ont été converti(s) en public", expiredTrajets.size());
        }
    }


    @Override
    @Transactional
    public void convertToPublic(Long trajetId) {
        Trajet trajet = getTrajetEntityById(trajetId);
        // Regle metier : un trajet deja public ne peut pas etre converti en public.
        if (trajet.getTypeTrajet() == TypeTrajet.PUBLIC) {
            throw new BusinessException("Le trajet est déjà public.");
        }
        trajet.setTypeTrajet(TypeTrajet.PUBLIC);
        trajet.setDateConversionPublic(LocalDateTime.now());
        trajetRepository.save(trajet);
        voyageurService.addRestriction(trajet.getOrganisateur().getId());
        log.info("Trajet {} converti en public, restriction appliquée à l'organisateur {}", trajetId, trajet.getOrganisateur().getId());
    }

    @Override
    @Transactional
    public void incrementPassagers(Long trajetId) {
        Trajet trajet = getTrajetEntityById(trajetId);
        incrementPassagersForTrajet(trajet, true);
    }

    private void incrementPassagersForTrajet(Trajet trajet, boolean save) {
        // Regle metier : un trajet de transport doit contenir exactement 4 passagers.
        // Des que les 4 places sont occupees, le trajet devient complet.
        if (trajet.getNbPassagersActuels() >= REQUIRED_PASSAGERS_PER_TRAJET) {
            throw new BusinessException("Le trajet est déjà complet.");
        }
        trajet.setNbPassagersActuels(trajet.getNbPassagersActuels() + 1);
        if (trajet.getNbPassagersActuels() == REQUIRED_PASSAGERS_PER_TRAJET) {
            trajet.setStatut(StatutTrajet.COMPLET);
        }
        if (save) {
            trajetRepository.save(trajet);
        }
    }

    @Override
    @Transactional
    public void decrementPassagers(Long trajetId) {
        Trajet trajet = getTrajetEntityById(trajetId);
        // Regle metier : impossible de retirer un passager d'un trajet vide.
        if (trajet.getNbPassagersActuels() <= 0) {
            throw new BusinessException("Le trajet n'a aucun passager.");
        }
        trajet.setNbPassagersActuels(trajet.getNbPassagersActuels() - 1);
        if (trajet.getStatut() == StatutTrajet.COMPLET) {
            trajet.setStatut(StatutTrajet.PLANIFIE);
        }
        trajetRepository.save(trajet);
    }
    //la règle des 15 minutes entre deux trajets
    private boolean isSlotAvailable(Vehicule vehicule, LocalDateTime start, LocalDateTime end) {
        int breakMinutes = vehicule.getAgence().getDureeEntreTrajetMinutes();
        List<Trajet> existingTrajets = trajetRepository.findOverlappingTrajets(
                vehicule.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutTrajet.ANNULE
        );
        return existingTrajets.isEmpty();
    }

    private boolean isSlotAvailableExcluding(Vehicule vehicule, LocalDateTime start, LocalDateTime end, Long excludedTrajetId) {
        int breakMinutes = vehicule.getAgence().getDureeEntreTrajetMinutes();
        List<Trajet> existingTrajets = trajetRepository.findOverlappingTrajets(
                vehicule.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutTrajet.ANNULE
        );
        return existingTrajets.stream().allMatch(m -> m.getId().equals(excludedTrajetId));
    }

    private void validateBookingDelay(Voyageur voyageur, LocalDate trajetDate) {
        long daysUntilTrajet = ChronoUnit.DAYS.between(LocalDate.now(), trajetDate);
        int maxDaysAhead = switch (voyageur.getTypeVoyageur()) {
            case GLOBAL -> 21;
            case AGENCE -> 14;
            case LIBRE -> 5;
        };

        // Regle metier : la date du trajet ne peut pas etre dans le passe.
        if (daysUntilTrajet < 0) {
            throw new BusinessException("La date du trajet est déjà passée.");
        }
        // Regle metier : le delai de reservation depend du type de voyageur.
        if (daysUntilTrajet > maxDaysAhead) {
            throw new BusinessException("Le voyageur " + voyageur.getTypeVoyageur()
                    + " ne peut pas réserver plus de " + maxDaysAhead + " jours à l'avance.");
        }
    }
    //methode fermeture globaux de agence
    private void validateAgenceNotClosed(Vehicule vehicule, LocalDate date) {
        boolean closedGlobally = indisponibiliteServiceRepository.existsByDateAndGlobalTrue(date);
        boolean closedForAgence = indisponibiliteServiceRepository.existsByDateAndAgenceId(date, vehicule.getAgence().getId());
        // Regle metier : aucun trajet ne peut etre cree sur une agence ferme .jour fermeture globaux.
        if (closedGlobally || closedForAgence) {
            throw new BusinessException("Le agence est fermé à la date : " + date);
        }
    }
        //methode les horaires d'ouverture propre pour une agence
    private void validateAgenceOpeningHours(Vehicule vehicule, LocalTime heureDebut, LocalTime heureFin) {
        LocalTime openingTime = vehicule.getAgence().getHeureOuverture();
        LocalTime closingTime = vehicule.getAgence().getHeureFermeture();
        // Regle metier : le trajet doit respecter les horaires d'ouverture de cette agence.
        if (heureDebut.isBefore(openingTime) || heureFin.isAfter(closingTime)) {
            throw new BusinessException("Le trajet est en dehors des heures d'ouverture de cette agence (" + openingTime + " - " + closingTime + ").");
        }
    }

    private void validateAgenceAccessForOrganizer(Voyageur organisateur, Vehicule vehicule) {
        if (organisateur.getTypeVoyageur() == TypeVoyageur.AGENCE) {
            // Regle metier : un voyageur AGENCE ne peut creer un trajet que sur sa propre agence.
            if (organisateur.getAgence() == null || !organisateur.getAgence().getId().equals(vehicule.getAgence().getId())) {
                throw new BusinessException("Un voyageur AGENCE ne peut créer un trajet que sur sa propre agence.");
            }
        }
    }

    private void createPendingReservationForOrganizer(Trajet trajet, Voyageur organisateur) {
        Reservation reservation = Reservation.builder()
                .trajet(trajet)
                .voyageur(organisateur)
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        reservation = reservationRepository.save(reservation);

        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(trajet.getPrixParPassager())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);

        // Regle metier : la reservation en attente de l'organisateur bloque une place.
        incrementPassagersForTrajet(trajet, false);
    }
}
