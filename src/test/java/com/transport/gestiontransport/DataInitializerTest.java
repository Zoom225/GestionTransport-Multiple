package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_shouldBackfillLegacyVoyageurPasswordHashes() {
        Voyageur legacyVoyageur = Voyageur.builder()
                .matricule("g1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas@email.com")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .passwordHash(null)
                .build();

        Voyageur restrictedVoyageur = Voyageur.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo@email.com")
                .typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0)
                .passwordHash("existing-hash")
                .build();
        restrictedVoyageur.setId(2L);

        when(agenceRepository.count()).thenReturn(1L);
        when(trajetRepository.count()).thenReturn(1L);
        when(voyageurRepository.findAll()).thenReturn(List.of(legacyVoyageur, restrictedVoyageur));
        when(passwordEncoder.encode("Voyageur1234!")).thenReturn("hashed-default-password");
        when(voyageurRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(restrictedVoyageur));
        when(restrictionRepository.existsByVoyageurIdAndDateFinAfter(eq(2L), any(LocalDate.class))).thenReturn(true);

        dataInitializer.run();

        ArgumentCaptor<List<Voyageur>> captor = ArgumentCaptor.forClass(List.class);
        verify(voyageurRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Voyageur::getPasswordHash)
                .contains("hashed-default-password");
        assertThat(legacyVoyageur.getPasswordHash()).isEqualTo("hashed-default-password");
    }
}
