package com.transport.gestiontransport.mapper;

import com.transport.gestiontransport.dto.request.VoyageurRequest;
import com.transport.gestiontransport.dto.response.VoyageurResponse;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de VoyageurMapper")
class VoyageurMapperTest {

    private VoyageurMapper voyageurMapper;

    @BeforeEach
    void setUp() {
        voyageurMapper = new VoyageurMapper();
    }

    @Test
    @DisplayName("Doit mapper VoyageurRequest vers Voyageur Entity")
    void shouldMapVoyageurRequestToVoyageurEntity() {
        // Arrange
        VoyageurRequest request = VoyageurRequest.builder()
                .matricule("L12345")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .typeVoyageur(TypeVoyageur.LIBRE)
                .build();

        // Act
        Voyageur voyageur = voyageurMapper.toEntity(request);

        // Assert
        assertNotNull(voyageur);
        assertAll("Vérification du mapping de Request vers Entity",
                () -> assertEquals("L12345", voyageur.getMatricule()),
                () -> assertEquals("Doe", voyageur.getNom()),
                () -> assertEquals("John", voyageur.getPrenom()),
                () -> assertEquals("john.doe@example.com", voyageur.getEmail()),
                () -> assertEquals(TypeVoyageur.LIBRE, voyageur.getTypeVoyageur()),
                () -> assertNull(voyageur.getAgence(), "Le agence ne doit pas être mappé par le mapper")
        );
    }

    @Test
    @DisplayName("Doit mapper Voyageur Entity vers VoyageurResponse")
    void shouldMapVoyageurEntityToVoyageurResponse() {
        // Arrange
        Agence agence = new Agence();
        agence.setId(1L);
        agence.setNom("Agence Central");

        // Correction : Construire l'objet, puis setter l'ID
        Voyageur voyageur = Voyageur.builder()
                .matricule("S54321")
                .nom("Smith")
                .prenom("Jane")
                .email("jane.smith@example.com")
                .typeVoyageur(TypeVoyageur.AGENCE)
                .agence(agence)
                .solde(25.5)
                .build();
        voyageur.setId(1L); // Simule l'ID généré par la base de données

        // Act
        VoyageurResponse response = voyageurMapper.toResponse(voyageur);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping de Entity vers Response",
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("S54321", response.getMatricule()),
                () -> assertEquals("Smith", response.getNom()),
                () -> assertEquals("Jane", response.getPrenom()),
                () -> assertEquals(TypeVoyageur.AGENCE, response.getTypeVoyageur()),
                () -> assertEquals(1L, response.getAgenceId()),
                () -> assertEquals("Agence Central", response.getAgenceNom()),
                () -> assertEquals(25.5, response.getSolde())
        );
    }

    @Test
    @DisplayName("Doit gérer un Agence null lors du mapping vers VoyageurResponse")
    void shouldHandleNullAgenceWhenMappingToVoyageurResponse() {
        // Arrange
        // Correction : Construire l'objet, puis setter l'ID
        Voyageur voyageur = Voyageur.builder()
                .matricule("G9876")
                .typeVoyageur(TypeVoyageur.GLOBAL) // Un voyageur GLOBAL n'a pas de agence
                .agence(null)
                .solde(0.0) // Initialiser tous les champs non-null
                .build();
        voyageur.setId(2L);

        // Act
        VoyageurResponse response = voyageurMapper.toResponse(voyageur);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping avec une agence null",
                () -> assertEquals(2L, response.getId()),
                () -> assertNull(response.getAgenceId()),
                () -> assertNull(response.getAgenceNom())
        );
    }
}
