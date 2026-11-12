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
import static org.mockito.ArgumentMatchers.argThat;
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

    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private ReservationService reservationService;
    @Mock private com.transport.gestiontransport.service.impl.AdminAuthorizationService adminAuthorizationService;

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
        organisateur.setPeutProposerTrajet(true);
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
                "PUBLIC", com.transport.gestiontransport.entity.enums.VilleBelge.MONS,
                com.transport.gestiontransport.entity.enums.VilleBelge.BRUXELLES, false, null
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
                0,
                25.0,
                null  // dateConversionPublic
        );
    }

    @Test
    void nePublieJamaisUnTrajetPriveSansConsentement() {
        Trajet prive = Trajet.builder().typeTrajet(TypeTrajet.PRIVE).statut(StatutTrajet.PLANIFIE)
                .dateDebut(LocalDateTime.now().plusHours(12)).publicationAutomatiqueAutorisee(false).build();
        when(trajetRepository.findById(10L)).thenReturn(Optional.of(prive));
        assertThrows(BusinessException.class, () -> trajetService.convertToPublic(10L));
        assertEquals(TypeTrajet.PRIVE, prive.getTypeTrajet());
        verifyNoInteractions(voyageurService);
        verify(trajetRepository, never()).save(any());
    }

    @Test
    void refuseUnChauffeurNonAutorise() {
        organisateur.setPeutProposerTrajet(false);
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(1L)).thenReturn(vehicule);
        assertThrows(BusinessException.class, () -> trajetService.createTrajet(createTrajetRequest, "user123"));
        verify(trajetRepository, never()).save(any());
    }

    @Test
    void refuseDeuxVillesIdentiques() {
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(1L)).thenReturn(vehicule);
        var request = new CreateTrajetRequest(1L, createTrajetRequest.trajetDate(), "PUBLIC",
                com.transport.gestiontransport.entity.enums.VilleBelge.MONS,
                com.transport.gestiontransport.entity.enums.VilleBelge.MONS, false, null);
        assertThrows(BusinessException.class, () -> trajetService.createTrajet(request, "user123"));
        verify(trajetRepository, never()).save(any());
    }

    @Test
    void unAdministrateurDoitSelectionnerUnChauffeur() {
        assertThrows(BusinessException.class, () -> trajetService.createTrajetForAdmin(createTrajetRequest));
        verifyNoInteractions(trajetRepository);
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
        when(trajetMapper.toTrajetDto(any(Trajet.class))).thenReturn(trajetDto); // Utiliser le DTO corrigé

        // Act
        TrajetDto result = trajetService.createTrajet(createTrajetRequest, "user123");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.nbPassagersActuels());
        assertEquals(TypeTrajet.PUBLIC, result.typeTrajet());
        assertEquals(25.0, result.prixParPassager());
        verify(trajetRepository, times(1)).save(argThat(trajet ->
                Double.valueOf(100.0).equals(trajet.getPrixTotal())
                        && Double.valueOf(25.0).equals(trajet.getPrixParPassager())));
        verifyNoInteractions(reservationRepository, paiementRepository);
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
    @DisplayName("createTrajet - Échoue si le départ est passé")
    void createTrajet_ShouldFail_WhenDepartureIsPast() {
        // Arrange
        CreateTrajetRequest tooFarRequest = new CreateTrajetRequest(vehicule.getId(), LocalDateTime.now().minusDays(1), "PUBLIC", com.transport.gestiontransport.entity.enums.VilleBelge.MONS, com.transport.gestiontransport.entity.enums.VilleBelge.BRUXELLES, false, null);
        when(voyageurRepository.findByMatriculeIgnoreCase("user123")).thenReturn(Optional.of(organisateur));
        when(vehiculeService.getById(vehicule.getId())).thenReturn(vehicule);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            trajetService.createTrajet(tooFarRequest, "user123");
        });
        assertTrue(exception.getMessage().contains("départ doit être futur"));
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
