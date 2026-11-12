package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import com.transport.gestiontransport.dto.response.TrajetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de TrajetMapper")
class TrajetMapperTest {

    private TrajetMapper trajetMapper;

    private Trajet trajet;
    private Voyageur organisateur;
    private Vehicule vehicule;
    private Agence agence;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 5, 20, 14, 0);

    @BeforeEach
    void setUp() {
        trajetMapper = new TrajetMapper();

        agence = new Agence();
        agence.setId(1L);
        agence.setNom("Test Agence");
        agence.setDureeTrajetMinutes(90);
        agence.setHeureOuverture(LocalTime.of(9, 0));
        agence.setHeureFermeture(LocalTime.of(22, 0));

        organisateur = new Voyageur();
        organisateur.setId(1L);
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");

        vehicule = new Vehicule();
        vehicule.setId(1L);
        vehicule.setNom("Court Central");
        vehicule.setPrix(20.0);
        vehicule.setAgence(agence);

        trajet = new Trajet();
        trajet.setId(1L);
        trajet.setVehicule(vehicule);
        trajet.setChauffeur(organisateur);
        trajet.setDateDebut(FIXED_DATE);
        trajet.setDateFin(FIXED_DATE.plusMinutes(agence.getDureeTrajetMinutes()));
        trajet.setTypeTrajet(TypeTrajet.PUBLIC);
        trajet.setStatut(StatutTrajet.PLANIFIE);
        trajet.setNbPassagersActuels(1);
        trajet.setPrixParPassager(5.0);
    }

    @Test
    @DisplayName("toTrajetDto - Doit mapper correctement Trajet vers TrajetDto")
    void toTrajetDto_shouldMapTrajetToTrajetDto() {
        TrajetDto resultDto = trajetMapper.toTrajetDto(trajet);

        assertNotNull(resultDto);
        assertAll("Vérification des propriétés de TrajetDto",
                () -> assertEquals(trajet.getId(), resultDto.id()),
                () -> assertEquals(trajet.getVehicule().getId(), resultDto.vehiculeId()),
                () -> assertEquals(trajet.getVehicule().getNom(), resultDto.vehiculeNom()),
                () -> assertEquals(agence.getNom(), resultDto.agenceNom()),
                () -> assertEquals(trajet.getChauffeur().getId(), resultDto.organisateurId()),
                () -> assertEquals("John Doe", resultDto.organisateurNom(), "Le nom de l'organisateur doit être concaténé"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut()),
                () -> assertEquals(FIXED_DATE.plusMinutes(90).toLocalTime(), resultDto.heureFin()),
                () -> assertEquals(trajet.getTypeTrajet(), resultDto.typeTrajet()),
                () -> assertEquals(trajet.getStatut(), resultDto.statut()),
                () -> assertEquals(1, resultDto.nbPassagersActuels(), "Le nombre de passagers doit être 1"),
                () -> assertEquals(trajet.getPrixParPassager(), resultDto.prixParPassager()),
                () -> assertNull(resultDto.dateConversionPublic())
        );
    }

    @Test
    @DisplayName("toTrajetDto - Doit retourner null si le Trajet est null")
    void toTrajetDto_shouldReturnNull_whenTrajetIsNull() {
        TrajetDto resultDto = trajetMapper.toTrajetDto(null);

        assertNull(resultDto, "Le DTO résultant doit être null quand l'entité source est null");
    }

    @Test
    @DisplayName("toTrajetDto - Doit gérer les propriétés nulles sans erreur")
    void toTrajetDto_shouldHandleNullProperties() {
        Trajet trajetWithNulls = new Trajet();
        trajetWithNulls.setId(2L);
        trajetWithNulls.setVehicule(null);
        trajetWithNulls.setChauffeur(null);
        trajetWithNulls.setDateDebut(FIXED_DATE);
        trajetWithNulls.setDateFin(FIXED_DATE.plusMinutes(90));
        trajetWithNulls.setNbPassagersActuels(0);

        TrajetDto resultDto = trajetMapper.toTrajetDto(trajetWithNulls);

        assertNotNull(resultDto);
        assertAll("Vérification du mapping avec des propriétés nulles",
                () -> assertEquals(2L, resultDto.id()),
                () -> assertNull(resultDto.vehiculeId(), "L'ID du vehicule doit être null"),
                () -> assertNull(resultDto.vehiculeNom(), "Le nom du vehicule doit être null"),
                () -> assertNull(resultDto.agenceNom(), "Le nom de cette agence doit être null"),
                () -> assertNull(resultDto.organisateurId(), "L'ID de l'organisateur doit être null"),
                () -> assertNull(resultDto.organisateurNom(), "Le nom de l'organisateur doit être null"),
                () -> assertEquals(0, resultDto.nbPassagersActuels(), "Le nombre de passagers doit être 0"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut())
        );
    }
}
