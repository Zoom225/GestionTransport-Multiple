package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.TrajetRepository;
import com.transport.gestiontransport.service.impl.AdminAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de AdminAuthorizationService")
class AdminAuthorizationServiceTest {

    @Mock
    private AdministrateurRepository administrateurRepository;
    @Mock
    private TrajetRepository trajetRepository;

    private AdminAuthorizationService adminAuthorizationService;
    private Agence agenceLyon;
    private Agence agenceParis;

    @BeforeEach
    void setUp() {
        adminAuthorizationService = new AdminAuthorizationService(administrateurRepository, trajetRepository);
        agenceLyon = Agence.builder().nom("Agence Lyon").build();
        agenceLyon.setId(1L);
        agenceParis = Agence.builder().nom("Agence Paris").build();
        agenceParis.setId(2L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un admin GLOBAL peut accéder à toutes les agences")
    void globalAdminCanAccessEveryAgence() {
        String adminEmail = "admin@transport.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.GLOBAL, null)));

        adminAuthorizationService.checkAgenceAccess(agenceParis.getId());
    }

    @Test
    @DisplayName("Un admin AGENCE peut accéder uniquement à son agence")
    void agenceAdminCanAccessOwnAgenceOnly() {
        String adminEmail = "admin.lyon@transport.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.AGENCE, agenceLyon)));

        adminAuthorizationService.checkAgenceAccess(agenceLyon.getId());
        assertThatThrownBy(() -> adminAuthorizationService.checkAgenceAccess(agenceParis.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sa propre agence");
    }

    @Test
    @DisplayName("Un admin AGENCE sans agence rattaché est refusé")
    void agenceAdminWithoutAgenceIsRejected() {
        String adminEmail = "admin.agence@transport.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.AGENCE, null)));

        assertThatThrownBy(() -> adminAuthorizationService.checkAgenceAccess(agenceLyon.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sa propre agence");
    }

    @Test
    @DisplayName("Un admin AGENCE ne peut gérer que les voyageurs de son agence")
    void agenceAdminCanManageOnlyOwnVoyageurs() {
        String adminEmail = "admin.lyon@transport.com";
        authenticate(adminEmail);
        // La ligne suivante est la ligne 94 dans le fichier de test si le reste est identique.
        // Elle doit stuber avec 'adminEmail' qui est "admin.lyon@transport.com"
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.AGENCE, agenceLyon)));

        Voyageur voyageurParis = Voyageur.builder().agence(agenceParis).build();

        assertThatThrownBy(() -> adminAuthorizationService.checkVoyageurAccess(voyageurParis))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("voyageurs de son agence");
    }

    @Test
    @DisplayName("Un admin AGENCE ne peut gérer que les vehicules de son agence")
    void agenceAdminCanManageOnlyOwnVehicules() {
        String adminEmail = "admin.lyon@transport.com";
        authenticate(adminEmail);
        when(administrateurRepository.findByEmail(adminEmail))
                .thenReturn(Optional.of(admin(adminEmail, TypeAdministrateur.AGENCE, agenceLyon)));

        Vehicule vehiculeParis = Vehicule.builder().agence(agenceParis).build();

        assertThatThrownBy(() -> adminAuthorizationService.checkVehiculeAccess(vehiculeParis))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sa propre agence");
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null)
        );
    }

    // Méthode admin modifiée pour accepter l'email
    private Administrateur admin(String email, TypeAdministrateur type, Agence agence) {
        return Administrateur.builder()
                .email(email) // Utilise l'email passé en paramètre
                .typeAdministrateur(type)
                .agence(agence)
                .build();
    }
}