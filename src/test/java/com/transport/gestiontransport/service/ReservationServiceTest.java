package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.PaiementRepository;
import com.transport.gestiontransport.repository.ReservationRepository;
import com.transport.gestiontransport.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ReservationService")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private TrajetService trajetService;

    @Mock
    private VoyageurService voyageurService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Agence agence;
    private Agence agenceB;
    private Vehicule vehicule;
    private Voyageur organisateur;
    private Voyageur passager;
    private Voyageur passagerAgenceB;
    private Trajet trajetPrive;
    private Trajet trajetPublic;
    private Trajet trajetComplet;

    @BeforeEach
    void setUp() {
        agenceB = Agence.builder()
                .nom("Agence Paris")
                .build();
        agenceB.setId(2L);

        agence = Agence.builder()
                .nom("Agence Lyon")
                .build();
        agence.setId(1L);

        vehicule = Vehicule.builder()
                .nom("Court A")
                .agence(agence)
                .build();
        vehicule.setId(1L);

        organisateur = Voyageur.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .agence(null)
                .build();
        organisateur.setId(1L);

        passager = Voyageur.builder()
                .matricule("G1002")
                .nom("Dupont")
                .prenom("Julie")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .agence(null)
                .build();
        passager.setId(2L);

        // voyageur rattaché à un autre agence → ne peut pas réserver sur agenceLyon
        passagerAgenceB = Voyageur.builder()
                .matricule("S20001")
                .nom("Leclerc")
                .prenom("Paul")
                .typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0)
                .agence(agenceB)
                .build();
        passagerAgenceB.setId(3L);

        trajetPrive = Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(15, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(16, 30)))
                .typeTrajet(TypeTrajet.PRIVE)
                .statut(StatutTrajet.PLANIFIE)
                .nbPassagersActuels(1)
                .prixTotal(60.0)
                .prixParPassager(15.0)
                .build();
        trajetPrive.setId(10L);

        trajetPublic = Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(17, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(18, 30)))
                .typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE)
                .nbPassagersActuels(1)
                .prixTotal(60.0)
                .prixParPassager(15.0)
                .build();
        trajetPublic.setId(11L);

        trajetComplet = Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(19, 0)))
                .dateFin(java.time.LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.of(20, 30)))
                .typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.COMPLET)
                .nbPassagersActuels(4)
                .prixTotal(60.0)
                .prixParPassager(15.0)
                .build();
        trajetComplet.setId(12L);
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ doit créer une réservation pour un trajet PUBLIC avec un voyageur valide")
        void shouldCreateReservationForPublicTrajet() {
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reservation result = reservationService.create(11L, 2L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            assertThat(result.getVoyageur()).isEqualTo(passager);
            assertThat(result.getTrajet()).isEqualTo(trajetPublic);

            // vérifier que le paiement EN_ATTENTE est créé automatiquement
            verify(paiementRepository, times(1)).save(argThat(paiement ->
                    paiement.getMontant().equals(15.0) &&
                            paiement.getStatut() == StatutPaiement.EN_ATTENTE
            ));
            verify(trajetService).incrementPassagers(11L);
        }

        @Test
        @DisplayName("✅ doit créer une réservation pour un trajet PRIVE quand l'organisateur ajoute un passager")
        void shouldCreateReservationForPriveTrajetByOrganizer() {
            when(trajetService.getTrajetEntityById(10L)).thenReturn(trajetPrive); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(10L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // l'organisateur (id=1) ajoute passager (id=2) → autorisé
            Reservation result = reservationService.create(10L, 2L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            verify(trajetService).incrementPassagers(10L);
        }

        @Test
        @DisplayName("✅ doit créer des réservations à partir de matricules invités")
        void shouldCreateReservationsForInvites() {
            when(trajetService.getTrajetEntityById(10L)).thenReturn(trajetPrive);
            when(voyageurService.getByMatricule("g1002")).thenReturn(passager);
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(10L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = reservationService.createForInvites(10L, 1L, java.util.List.of("g1002"));

            assertThat(result.size()).isEqualTo(1);
            verify(voyageurService).getByMatricule("g1002");
            verify(trajetService).incrementPassagers(10L);
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le trajet est COMPLET")
        void shouldThrowWhenTrajetIsFull() {
            when(trajetService.getTrajetEntityById(12L)).thenReturn(trajetComplet); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);

            assertThatThrownBy(() -> reservationService.create(12L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà complet");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le trajet est ANNULE")
        void shouldThrowWhenTrajetIsCancelled() {
            trajetPublic.setStatut(StatutTrajet.ANNULE);
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("annulé");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le voyageur est déjà inscrit au trajet")
        void shouldThrowWhenVoyageurAlreadyRegistered() {
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà inscrit");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le voyageur a une restriction active")
        void shouldThrowWhenVoyageurHasActiveRestriction() {
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 2L)).thenReturn(false);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("restriction active");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le voyageur a un solde impayé")
        void shouldThrowWhenVoyageurHasOutstandingBalance() {
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 2L)).thenReturn(false);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("solde impayé");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un non-organisateur tente de rejoindre un trajet PRIVE")
        void shouldThrowWhenNonOrganizerJoinsPriveTrajet() {
            // passager (id=2) essaie de rejoindre directement un trajet privé
            // sans passer par l'organisateur (id=1)
            when(trajetService.getTrajetEntityById(10L)).thenReturn(trajetPrive); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(10L, 2L)).thenReturn(false);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(10L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("seul l'organisateur");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un voyageur AGENCE réserve sur le mauvais agence")
        void shouldThrowWhenAgenceVoyageurBooksOnWrongAgence() {
            // passagerAgenceB est rattaché à Paris → ne peut pas réserver sur Lyon
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(3L)).thenReturn(passagerAgenceB);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 3L)).thenReturn(false);
            when(voyageurService.hasActiveRestriction(3L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(3L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(11L, 3L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("propre agence");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ paiement créé avec le montant correct = prixParPassager du trajet")
        void shouldCreatePaiementWithCorrectAmount() {
            when(trajetService.getTrajetEntityById(11L)).thenReturn(trajetPublic); // MODIFIÉ
            when(voyageurService.getById(2L)).thenReturn(passager);
            when(reservationRepository.existsByTrajetIdAndVoyageurId(11L, 2L)).thenReturn(false);
            when(voyageurService.hasActiveRestriction(2L)).thenReturn(false);
            when(voyageurService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.create(11L, 2L, 2L);

            // prixParPassager = 60€ / 4 = 15€
            verify(paiementRepository).save(argThat(p ->
                    p.getMontant().equals(15.0) &&
                            p.getStatut() == StatutPaiement.EN_ATTENTE
            ));
            verify(trajetService).incrementPassagers(11L);
        }
    }

    // ================================================================
    // CANCEL
    // ================================================================
    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("✅ doit annuler la réservation en attente et décrémenter le nombre de passagers")
        void shouldCancelReservation() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE)
                    .build();

            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
            verify(trajetService).decrementPassagers(trajetPublic.getId());
        }

        @Test
        @DisplayName("✅ doit rembourser le paiement quand la réservation est annulée après paiement")
        void shouldRefundPaymentWhenCancelledAfterPayment() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.PAYE) // déjà payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.CONFIRMEE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement doit passer à REMBOURSE
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
            verify(paiementRepository, times(1)).save(paiement);
            verify(trajetService, times(1)).decrementPassagers(trajetPublic.getId());
        }

        @Test
        @DisplayName("✅ doit annuler le paiement quand la réservation est annulée avant paiement")
        void shouldCancelPaymentWhenNotYetPaid() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE) // pas encore payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement passe à ANNULE — réservation annulée avant paiement
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.ANNULE);
            verify(paiementRepository, times(1)).save(paiement);
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand la réservation est déjà annulée")
        void shouldThrowWhenAlreadyCancelled() {
            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.ANNULEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancel(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà annulée");

            verify(trajetService, never()).decrementPassagers(any());
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand la réservation n'est pas trouvée")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancel(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Réservation introuvable");
        }
    }

    // ================================================================
    // CONFIRM
    // ================================================================
    @Nested
    @DisplayName("confirm()")
    class ConfirmTests {

        @Test
        @DisplayName("✅ doit confirmer la réservation sans incrémenter à nouveau les passagers du trajet")
        void shouldConfirmReservation() {
            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.confirm(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
            verify(trajetService, never()).incrementPassagers(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand la réservation est déjà confirmée")
        void shouldThrowWhenAlreadyConfirmed() {
            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.CONFIRMEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.confirm(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà confirmée");

            verify(trajetService, never()).incrementPassagers(any());
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById()")
    class GetTests {

        @Test
        @DisplayName("✅ doit retourner la réservation quand l'id existe")
        void shouldReturnReservationById() {
            Reservation reservation = Reservation.builder()
                    .trajet(trajetPublic)
                    .voyageur(passager)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            Reservation result = reservationService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand l'id n'est pas trouvé")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Réservation introuvable");
        }
    }
}
