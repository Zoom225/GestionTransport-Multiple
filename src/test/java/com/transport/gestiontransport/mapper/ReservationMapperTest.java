package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.response.PaiementResponse;
import com.transport.gestiontransport.dto.response.ReservationResponse;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Paiement;
import com.transport.gestiontransport.entity.Reservation;
import com.transport.gestiontransport.entity.enums.StatutPaiement;
import com.transport.gestiontransport.entity.enums.StatutReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ReservationMapper")
class ReservationMapperTest {

    @Mock
    private PaiementMapper paiementMapper;

    @InjectMocks
    private ReservationMapper reservationMapper;

    private Reservation reservation;
    private Trajet trajet;
    private Voyageur voyageur;
    private Paiement paiement;

    @BeforeEach
    void setUp() {
        trajet = new Trajet();
        trajet.setId(1L);
        trajet.setDateDebut(LocalDateTime.of(2024, 8, 20, 19, 0));

        voyageur = new Voyageur();
        voyageur.setId(10L);
        voyageur.setNom("Doe");
        voyageur.setPrenom("John");

        paiement = new Paiement();
        paiement.setId(100L);
        paiement.setStatut(StatutPaiement.EN_ATTENTE);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setTrajet(trajet);
        reservation.setVoyageur(voyageur);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservation.setPaiement(paiement);
    }

    @Test
    @DisplayName("Doit mapper Reservation Entity vers ReservationResponse")
    void shouldMapReservationEntityToReservationResponse() {
        // Arrange
        PaiementResponse paiementResponse = PaiementResponse.builder()
                .id(100L)
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        when(paiementMapper.toResponse(paiement)).thenReturn(paiementResponse);

        // Act
        ReservationResponse response = reservationMapper.toResponse(reservation);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getTrajetId());
        assertEquals(LocalDateTime.of(2024, 8, 20, 19, 0), response.getTrajetDateTime());
        assertEquals(10L, response.getVoyageurId());
        assertEquals("John Doe", response.getVoyageurNom()); // Correction de l'ordre
        assertEquals(StatutReservation.EN_ATTENTE, response.getStatut());
        assertNotNull(response.getPaiement());
        assertEquals(100L, response.getPaiement().getId());
    }

    @Test
    @DisplayName("Doit gérer un Paiement null lors du mapping")
    void shouldHandleNullPaiementWhenMapping() {
        // Arrange
        reservation.setPaiement(null);

        // Act
        ReservationResponse response = reservationMapper.toResponse(reservation);

        // Assert
        assertNotNull(response);
        assertNull(response.getPaiement());
    }

    @Test
    @DisplayName("Doit lever une exception si Trajet ou Voyageur est null")
    void shouldThrowExceptionIfRequiredEntitiesAreNull() {
        // Arrange 1
        reservation.setTrajet(null);

        // Act & Assert 1
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            reservationMapper.toResponse(reservation);
        });
        assertEquals("La réservation doit avoir un trajet et un voyageur associés.", ex1.getMessage());

        // Arrange 2
        reservation.setTrajet(trajet); // Rétablir le trajet
        reservation.setVoyageur(null);

        // Act & Assert 2
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            reservationMapper.toResponse(reservation);
        });
        assertEquals("La réservation doit avoir un trajet et un voyageur associés.", ex2.getMessage());
    }
}
