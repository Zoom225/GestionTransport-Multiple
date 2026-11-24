package com.transport.gestiontransport.service;

import java.math.BigDecimal;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.impl.ReservationServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnulationTrajetTest {
    @Mock TrajetRepository trajets;
    @Mock ReservationRepository reservations;
    @Mock PaiementRepository paiements;
    @Mock VoyageurService voyageurs;
    @Mock EntityManager em;
    @InjectMocks ReservationServiceImpl service;
    Voyageur organisateur, chauffeur, client;
    Trajet trajet;

    @BeforeEach void preparer() {
        organisateur = Voyageur.builder().solde(new BigDecimal("100.00")).build(); organisateur.setId(1L);
        chauffeur = Voyageur.builder().solde(new BigDecimal("0.00")).build(); chauffeur.setId(2L);
        client = Voyageur.builder().solde(new BigDecimal("0.00")).build(); client.setId(3L);
        trajet = Trajet.builder().organisateur(organisateur).chauffeur(chauffeur)
                .dateDebut(LocalDateTime.now().plusHours(2)).statut(StatutTrajet.PLANIFIE)
                .nbPassagersActuels(0).penaliteIncompletImputeeLe(LocalDateTime.now().minusHours(1)).build();
        trajet.setId(10L);
        when(trajets.findById(10L)).thenReturn(Optional.of(trajet));
    }
    Reservation reservation(long id, StatutReservation statut, StatutPaiement statutPaiement) {
        Reservation r = Reservation.builder().trajet(trajet).voyageur(client).statut(statut)
                .paiement(Paiement.builder().montant(new BigDecimal("25.00")).statut(statutPaiement).build()).build();
        r.setId(id); when(reservations.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    @ParameterizedTest @ValueSource(longs={1,2})
    void organisateurEtChauffeurPeuventAnnulerAvantDepart(long auteur) {
        service.cancelTrajet(10L, auteur);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.ANNULE);
        assertThat(trajet.getDateAnnulation()).isNotNull();
        assertThat(trajet.getAnnuleParVoyageurId()).isEqualTo(auteur);
        assertThat(organisateur.getSolde()).isEqualByComparingTo("100.00");
        assertThat(trajet.getPenaliteIncompletImputeeLe()).isNotNull();
        verifyNoInteractions(voyageurs);
    }
    @Test void simplePassagerNePeutPasAnnulerLeTrajetEntier() {
        assertThatThrownBy(() -> service.cancelTrajet(10L, 3L)).isInstanceOf(BusinessException.class);
        verify(trajets, never()).save(any());
    }
    @ParameterizedTest @ValueSource(longs={0,-1})
    void refuseAuDepartOuApresMemeSansReservation(long secondes) {
        trajet.setDateDebut(LocalDateTime.now().plusSeconds(secondes));
        assertThatThrownBy(() -> service.cancelTrajet(10L, 2L)).isInstanceOf(BusinessException.class).hasMessageContaining("départ");
        verify(trajets, never()).save(any());
    }
    @Test void annulationTotaleRembourseEtAnnuleSansEffacerLaDette() {
        Reservation payee = reservation(20L, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        Reservation attente = reservation(21L, StatutReservation.EN_ATTENTE, StatutPaiement.EN_ATTENTE);
        trajet.setNbPassagersActuels(1);
        when(reservations.findByTrajetId(10L)).thenReturn(List.of(payee, attente));
        service.cancelTrajet(10L, 2L);
        assertThat(payee.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(payee.getPaiement().getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
        assertThat(attente.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(attente.getPaiement().getStatut()).isEqualTo(StatutPaiement.ANNULE);
        assertThat(trajet.getNbPassagersActuels()).isZero();
        assertThat(organisateur.getSolde()).isEqualByComparingTo("100.00");
        verifyNoInteractions(voyageurs);
    }
    @Test void clientPeutAnnulerSaReservationAvantDepart() {
        Reservation r = reservation(20L, StatutReservation.EN_ATTENTE, StatutPaiement.EN_ATTENTE);
        service.cancelForUser(20L, 3L);
        assertThat(r.getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }
    @Test void secondeAnnulationRefuseeSansChangerHistorique() {
        trajet.setStatut(StatutTrajet.ANNULE);
        assertThatThrownBy(() -> service.cancelTrajet(10L, 2L)).isInstanceOf(BusinessException.class);
        verify(trajets, never()).save(any());
    }
}
