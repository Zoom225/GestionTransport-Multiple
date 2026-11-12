package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.TrajetRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import com.transport.gestiontransport.repository.PaiementRepository;
import com.transport.gestiontransport.service.impl.PaiementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de PaiementService")
class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private VoyageurRepository voyageurRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @InjectMocks
    private PaiementServiceImpl paiementService;

    private Agence agence;
    private Vehicule vehicule;
    private Voyageur organisateur;
    private Voyageur passager;
    private Trajet trajet;
    private Reservation reservation;
    private Paiement paiement;

    @BeforeEach
    void setUp() {
        agence = Agence.builder().nom("Agence Lyon").build();
        vehicule = Vehicule.builder().nom("Court A").agence(agence).build();

        organisateur = Voyageur.builder()
                .matricule("G1001").nom("Martin").prenom("Lucas")
                .typeVoyageur(TypeVoyageur.GLOBAL).solde(0.0).build();
        organisateur.setId(1L);

        passager = Voyageur.builder()
                .matricule("G1002").nom("Dupont").prenom("Julie")
                .typeVoyageur(TypeVoyageur.GLOBAL).solde(0.0).build();
        passager.setId(2L);

        // Correction : Utiliser uniquement dateDebut et dateFin
        LocalDateTime trajetStart = LocalDate.now().plusDays(25).atTime(15, 0);
        trajet = Trajet.builder()
                .vehicule(vehicule)
                .organisateur(organisateur)
                .dateDebut(trajetStart)
                .dateFin(trajetStart.plusMinutes(90))
                .typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE)
                .nbPassagersActuels(1)
                .prixTotal(100.0)
                .prixParPassager(25.0)
                .build();
        trajet.setId(10L);

        reservation = Reservation.builder()
                .trajet(trajet).voyageur(passager).statut(StatutReservation.EN_ATTENTE).build();
        reservation.setId(1L);

        paiement = Paiement.builder()
                .reservation(reservation).montant(25.0).statut(StatutPaiement.EN_ATTENTE).build();
        paiement.setId(1L);

        reservation.setPaiement(paiement);
    }

    @Nested
    @DisplayName("pay()")
    class PayTests {
        @Test
        @DisplayName("✅ doit traiter le paiement et confirmer la réservation")
        void shouldProcessPayment() {
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            assertThat(result.getMontant()).isEqualTo(25.0);
            verify(reservationService).confirm(1L);
        }

        @Test
        @DisplayName("✅ doit ajouter le solde impayé et l'effacer")
        void shouldAddOutstandingBalanceToPayment() {
            passager.setSolde(15.0);
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
            when(voyageurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getMontant()).isEqualTo(40.0);
            assertThat(passager.getSolde()).isEqualTo(0.0);
            verify(voyageurRepository).save(passager);
        }

        @Test
        @DisplayName("❌ doit lever une exception quand déjà payé")
        void shouldThrowWhenAlreadyPaid() {
            paiement.setStatut(StatutPaiement.PAYE);
            when(reservationService.getById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> paiementService.pay(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà été effectué");
        }

        @Test
        @DisplayName("✅ doit autoriser le paiement quand la place réservée rend le trajet complet")
        void shouldAllowPaymentWhenReservedPlaceMakesTrajetFull() {
            trajet.setNbPassagersActuels(4);
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            verify(reservationService).confirm(1L);
        }
    }

    @Nested
    @DisplayName("getById() and getByReservationId()")
    class GetTests {
        @Test
        @DisplayName("✅ doit retourner le paiement quand l'id existe")
        void shouldReturnPaymentById() {
            when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
            Paiement result = paiementService.getById(1L);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("❌ doit lever une exception quand l'id du paiement n'est pas trouvé")
        void shouldThrowWhenPaymentNotFound() {
            when(paiementRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> paiementService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("checkUnpaidBeforeTrajet()")
    class SchedulerTests {
        @Test
        @DisplayName("✅ doit annuler la réservation impayée et ajouter le solde")
        void shouldCancelUnpaidReservationAndAddBalanceToOrganizer() {
            // Correction : Utiliser setDateDebut pour modifier la date du trajet
            LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
            trajet.setDateDebut(tomorrowStart);

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)).thenReturn(List.of(paiement));
            when(voyageurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paiementService.checkUnpaidBeforeTrajet();

            verify(reservationService).cancel(reservation.getId());
            assertThat(organisateur.getSolde()).isEqualTo(25.0);
            verify(voyageurRepository).save(organisateur);
        }

        @Test
        @DisplayName("✅ NE doit PAS annuler quand le trajet n'est pas demain")
        void shouldNotCancelWhenTrajetIsNotTomorrow() {
            // Correction : S'assurer que la date du trajet n'est pas demain
            trajet.setDateDebut(LocalDate.now().plusDays(5).atStartOfDay());

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)).thenReturn(List.of(paiement));

            paiementService.checkUnpaidBeforeTrajet();

            verify(reservationService, never()).cancel(any());
            verify(voyageurRepository, never()).save(any());
        }
    }
}
