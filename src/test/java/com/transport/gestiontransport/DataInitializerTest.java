package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock private AgenceRepository agenceRepository;
    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private VoyageurRepository voyageurRepository;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private IndisponibiliteServiceRepository indisponibiliteServiceRepository;
    @Mock private TrajetRepository trajetRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private DataInitializer dataInitializer;

    @Test
    void run_preserveLesComptesEtPaiementsDeTouteBaseNonVide() {
        when(agenceRepository.count()).thenReturn(1L);
        dataInitializer.run();
        verify(voyageurRepository, never()).save(any());
        verify(voyageurRepository, never()).saveAll(any());
        verify(administrateurRepository, never()).save(any());
        verify(paiementRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refuseUneBaseMetierMemeVide() {
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5440/transportService");
        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void demonstrationIsoleeNeReserveAucunePlacePourLeChauffeur() {
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5440/transport_demo_test");
        ReflectionTestUtils.setField(dataInitializer, "demoPassword", "OnlyForThisTest123!");
        when(passwordEncoder.encode(any())).thenReturn("hash-test");
        when(agenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vehiculeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(voyageurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(agenceRepository.count()).thenReturn(0L, 1L);
        dataInitializer.run();
        dataInitializer.run();
        verify(trajetRepository, times(8)).save(argThat(t -> t.getChauffeur() != null
                && Boolean.TRUE.equals(t.getChauffeur().getPeutProposerTrajet()) && t.getNbPassagersActuels() == 0
                && t.getVilleDepart() != t.getVilleArrivee() && t.getPrixParPassager() == 25.0));
        verify(reservationRepository, never()).save(any());
        verify(paiementRepository, never()).save(any());
    }

    @Test
    void demonstrationUtiliseLesTroisComptesTransportEtDesMotsDePasseDistinctsParRole() {
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5541/transport_demo_connexions");
        when(passwordEncoder.encode("Admin1234!")).thenReturn("hash-admin");
        when(passwordEncoder.encode("Voyage1234!")).thenReturn("hash-voyageur");
        when(agenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vehiculeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(voyageurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        dataInitializer.run();
        for (String email : new String[]{"admin@transp.com", "admin.lyon@transp.com", "admin.paris@transp.com"}) {
            verify(administrateurRepository).save(argThat(a -> email.equals(a.getEmail()) && "hash-admin".equals(a.getPasswordHash())));
        }
        verify(voyageurRepository, times(4)).save(argThat(v -> "hash-voyageur".equals(v.getPasswordHash())));
    }

    @Test
    void refuseLaMiseAJourDesComptesSurLaBaseMetier() {
        when(agenceRepository.count()).thenReturn(1L);
        ReflectionTestUtils.setField(dataInitializer, "updateCredentials", true);
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5440/transportService");
        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(passwordEncoder);
        verify(administrateurRepository, never()).findAll();
    }

    @Test
    void refuseUnDoublonDeMatriculeAvantTouteEcriture() {
        when(agenceRepository.count()).thenReturn(1L);
        ReflectionTestUtils.setField(dataInitializer, "updateCredentials", true);
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://127.0.0.1:5541/transport_schema_copy_test");
        when(administrateurRepository.findAll()).thenReturn(java.util.List.of(
                Administrateur.builder().email("admin@padel.com").build(),
                Administrateur.builder().email("admin.lyon@padel.com").build(),
                Administrateur.builder().email("admin.paris@padel.com").build()));
        Voyageur ancien = Voyageur.builder().matricule("G1001").typeVoyageur(com.transport.gestiontransport.entity.enums.TypeVoyageur.VIP).build();
        ancien.setId(1L);
        Voyageur nouveau = Voyageur.builder().matricule("V1001").typeVoyageur(com.transport.gestiontransport.entity.enums.TypeVoyageur.VIP).build();
        nouveau.setId(2L);
        when(voyageurRepository.findAll()).thenReturn(java.util.List.of(ancien, nouveau));
        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class).hasMessageContaining("doublon");
        verify(voyageurRepository, never()).saveAllAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refuseUnMotDePasseDepassantLaLimiteBcryptAvantTouteInsertion() {
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5440/transport_demo_test");
        ReflectionTestUtils.setField(dataInitializer, "demoPassword", "a".repeat(73));
        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class).hasMessageContaining("72 octets");
        verify(agenceRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
