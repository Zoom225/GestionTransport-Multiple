package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.mapper.TrajetMapper;
import com.transport.gestiontransport.dto.request.CreateTrajetRequest;
import com.transport.gestiontransport.dto.response.TrajetDto;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.impl.TrajetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de TrajetServiceImpl")
class TrajetServiceTest {

    @Mock
    private TrajetRepository trajetRepository;
    @Mock
    private VoyageurRepository voyageurRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private IndisponibiliteServiceRepository indisponibiliteServiceRepository;
    @Mock
    private VehiculeService vehiculeService;
    @Mock
    private VoyageurService voyageurService;
    @Mock
    private TrajetMapper trajetMapper;

    @InjectMocks
    private TrajetServiceImpl trajetService;

    private Voyageur organisateur;
    private Vehicule vehicule;
    private Agence agence;
    private CreateTrajetRequest createTrajetRequest;
    private TrajetDto trajetDto;

    @BeforeEach
    void setUp() {
        agence = new Agence();
        agence.setId(1L);
        agence.setDureeTrajetMinutes(90);
        agence.setDureeEntreTrajetMinutes(15);
        agence.setHeureOuverture(LocalTime.of(9, 0));
        agence.setHeureFermeture(LocalTime.of(22, 0));

        organisateur = new Voyageur();
        organisateur.setId(1L);
        organisateur.setMatricule("user123");
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");
        organisateur.setTypeVoyageur(TypeVoyageur.LIBRE);

        vehicule = new Vehicule();
        vehicule.setId(1L);
        vehicule.setNom("Court Central");
        vehicule.setPrix(20.0);
        vehicule.setAgence(agence);

        createTrajetRequest = new CreateTrajetRequest(
                vehicule.getId(),
                LocalDate.now().plusDays(5).atTime(10, 0),
                "PUBLIC"
        );

        trajetDto = new TrajetDto(
                1L,
                vehicule.getId(),
                vehicule.getNom(),
                null, // agenceNom
                organisateur.getId(),
                organisateur.getPrenom() + " " + organisateur.getNom(),
                createTrajetRequest.trajetDate().toLocalDate(),
                createTrajetRequest.trajetDate().toLocalTime(),
                createTrajetRequest.trajetDate().plusMinutes(90).toLocalTime(),
                TypeTrajet.PUBLIC,
                StatutTrajet.PLANIFIE,
                1,
                15.0,
                null  // dateConversionPublic
        );
    }

    @Test
    @DisplayName("createTrajet - Succès")
    void createTrajet_ShouldSucceed_WhenAllRulesAreMet() {
        // Arrange
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);
        when(voyageurService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(voyageurService.hasActiveRestriction(organisateur.getId())).thenReturn(false);
        when(trajetRepository.findOverlappingTrajets(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trajetMapper.toTrajetDto(any(Trajet.class))).thenReturn(trajetDto); // Utiliser le DTO corrigé

        // Act
        TrajetDto result = trajetService.createTrajet(createTrajetRequest, "user123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.nbPassagersActuels());
        assertEquals(TypeTrajet.PUBLIC, result.typeTrajet());
        verify(trajetRepository, times(1)).save(any(Trajet.class));
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(paiementRepository, times(1)).save(any(Paiement.class));
    }

    @Test
    @DisplayName("createTrajet - Échoue si le voyageur a un solde impayé")
    void createTrajet_ShouldFail_WhenVoyageurHasOutstandingBalance() {
        // Arrange
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);
        when(voyageurService.hasOutstandingBalance(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            trajetService.createTrajet(createTrajetRequest, "user123");
        });
        assertEquals("Vous avez un solde impayé et ne pouvez pas créer de trajet.", exception.getMessage());
        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTrajet - Échoue si le voyageur a une restriction active")
    void createTrajet_ShouldFail_WhenVoyageurHasActiveRestriction() {
        // Arrange
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);
        when(voyageurService.hasOutstandingBalance(organisateur.getId())).thenReturn(false);
        when(voyageurService.hasActiveRestriction(organisateur.getId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            trajetService.createTrajet(createTrajetRequest, "user123");
        });
        assertEquals("Vous avez une restriction active et ne pouvez pas créer de trajet.", exception.getMessage());
    }

    @Test
    @DisplayName("createTrajet - Échoue si le délai de réservation n'est pas respecté")
    void createTrajet_ShouldFail_WhenBookingDelayIsNotMet() {
        // Arrange
        CreateTrajetRequest tooFarRequest = new CreateTrajetRequest(vehicule.getId(), LocalDateTime.now().plusDays(6), "PUBLIC");
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            trajetService.createTrajet(tooFarRequest, "user123");
        });
        assertTrue(exception.getMessage().contains("ne peut pas réserver plus de 5 jours à l'avance"));
    }

    @Test
    @DisplayName("createTrajet - Échoue si le créneau est déjà pris")
    void createTrajet_ShouldFail_WhenSlotIsAlreadyBooked() {
        // Arrange
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);
        when(trajetRepository.findOverlappingTrajets(any(), any(), any(), any())).thenReturn(Collections.singletonList(new Trajet()));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            trajetService.createTrajet(createTrajetRequest, "user123");
        });
        assertEquals("Ce créneau est déjà réservé sur le vehicule : " + vehicule.getId(), exception.getMessage());
    }

    @Test
    @DisplayName("createTrajet - Échoue si le voyageur n'est pas trouvé")
    void createTrajet_ShouldFail_WhenVoyageurNotFound() {
        // Arrange
        when(voyageurRepository.findByMatriculeIgnoreCase("unknownUser")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            trajetService.createTrajet(createTrajetRequest, "unknownUser");
        });
        assertEquals("Voyageur non trouvé pour l'utilisateur: unknownUser", exception.getMessage());
    }
}
