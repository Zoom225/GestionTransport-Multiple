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
    void refuseUnMotDePasseDepassantLaLimiteBcryptAvantTouteInsertion() {
        ReflectionTestUtils.setField(dataInitializer, "databaseUrl", "jdbc:postgresql://localhost:5440/transport_demo_test");
        ReflectionTestUtils.setField(dataInitializer, "demoPassword", "a".repeat(73));
        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class).hasMessageContaining("72 octets");
        verify(agenceRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
