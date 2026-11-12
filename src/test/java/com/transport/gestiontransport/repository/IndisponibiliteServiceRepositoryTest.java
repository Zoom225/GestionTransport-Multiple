package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.IndisponibiliteService;
import com.transport.gestiontransport.entity.Agence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests de IndisponibiliteServiceRepository")
class IndisponibiliteServiceRepositoryTest {

    @Autowired
    private IndisponibiliteServiceRepository indisponibiliteServiceRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Test
    @DisplayName("existsByDateAndGlobalTrue détecte une indisponibilité globale")
    void shouldFindGlobalClosingDay() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        indisponibiliteServiceRepository.save(IndisponibiliteService.builder()
                .date(date)
                .raison("Noël")
                .global(true)
                .build());

        assertThat(indisponibiliteServiceRepository.existsByDateAndGlobalTrue(date)).isTrue();
    }

    @Test
    @DisplayName("existsByDateAndAgenceId détecte une indisponibilité de agence")
    void shouldFindAgenceClosingDay() {
        Agence agence = agenceRepository.save(agence());
        LocalDate date = LocalDate.of(2026, 7, 14);
        indisponibiliteServiceRepository.save(IndisponibiliteService.builder()
                .date(date)
                .raison("Maintenance")
                .global(false)
                .agence(agence)
                .build());

        assertThat(indisponibiliteServiceRepository.existsByDateAndAgenceId(date, agence.getId())).isTrue();
    }

    private Agence agence() {
        return Agence.builder()
                .nom("Agence Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeTrajetMinutes(90)
                .dureeEntreTrajetMinutes(15)
                .anneeCivile(2026)
                .build();
    }
}
