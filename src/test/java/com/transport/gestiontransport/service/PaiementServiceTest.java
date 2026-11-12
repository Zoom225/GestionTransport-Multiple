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
                .typeVoyageur(TypeVoyageur.VIP).solde(0.0).build();
        organisateur.setId(1L);

        passager = Voyageur.builder()
                .matricule("G1002").nom("Dupont").prenom("Julie")
                .typeVoyageur(TypeVoyageur.VIP).solde(0.0).build();
        passager.setId(2L);

        // Correction : Utiliser uniquement dateDebut et dateFin
        LocalDateTime trajetStart = LocalDate.now().plusDays(25).atTime(15, 0);
        trajet = Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
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
            when(reservationService.preparePaiement(1L, 2L)).thenReturn(reservation);
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            assertThat(result.getMontant()).isEqualTo(25.0);
            verify(reservationService).confirm(1L);
        }

        @Test
        @DisplayName("✅ doit conserver une dette et refuser le paiement")
        void shouldAddOutstandingBalanceToPayment() {
            passager.setSolde(15.0);
            when(reservationService.preparePaiement(1L, 2L)).thenThrow(new BusinessException("solde impayé"));
            assertThatThrownBy(() -> paiementService.pay(1L, 2L)).isInstanceOf(BusinessException.class);
            assertThat(passager.getSolde()).isEqualTo(15.0);
            assertThat(paiement.getMontant()).isEqualTo(25.0);
            verifyNoInteractions(voyageurRepository, paiementRepository);
        }

        @Test
        @DisplayName("❌ doit lever une exception quand déjà payé")
        void shouldThrowWhenAlreadyPaid() {
            when(reservationService.preparePaiement(1L, 2L)).thenThrow(new BusinessException("déjà payé"));
            assertThatThrownBy(() -> paiementService.pay(1L, 2L)).isInstanceOf(BusinessException.class);
            verifyNoInteractions(paiementRepository);
        }

        @Test
        @DisplayName("✅ doit refuser le paiement quand le trajet est complet")
        void shouldAllowPaymentWhenReservedPlaceMakesTrajetFull() {
            when(reservationService.preparePaiement(1L, 2L)).thenThrow(new BusinessException("trajet complet"));
            assertThatThrownBy(() -> paiementService.pay(1L, 2L)).isInstanceOf(BusinessException.class);
            verify(reservationService, never()).confirm(any());
            verifyNoInteractions(paiementRepository);
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
        @DisplayName("✅ doit expirer les demandes sans facturer le chauffeur")
        void shouldCancelUnpaidReservationAndAddBalanceToOrganizer() {
            paiementService.checkUnpaidBeforeTrajet();
            verify(reservationService).expirePendingReservations();
            assertThat(organisateur.getSolde()).isEqualTo(0.0);
            verifyNoInteractions(voyageurRepository);
        }

        @Test
        @DisplayName("✅ doit utiliser le délai configurable et conserver les soldes")
        void shouldNotCancelWhenTrajetIsNotTomorrow() {
            paiementService.checkUnpaidBeforeTrajet();
            verify(reservationService).expirePendingReservations();
            verify(reservationService, never()).cancel(any());
            verifyNoInteractions(voyageurRepository);
        }
    }
}
