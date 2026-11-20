package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.exception.ParticipationException;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.impl.VoyageurEligibilityService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoyageurEligibilityServiceTest {
    final VoyageurRepository voyageurs = mock(VoyageurRepository.class);
    final RestrictionRepository restrictions = mock(RestrictionRepository.class);
    final Instant debut = Instant.parse("2026-03-27T12:00:00Z");
    final Instant fin = debut.plus(Duration.ofHours(168));

    private VoyageurEligibilityService service(double solde, Instant instant) {
        Voyageur v = Voyageur.builder().solde(solde).build(); v.setId(1L);
        when(voyageurs.findById(1L)).thenReturn(Optional.of(v));
        when(voyageurs.findByIdForUpdate(1L)).thenReturn(Optional.of(v));
        when(voyageurs.soldeCourant(1L)).thenReturn(solde);
        when(restrictions.finSuspensionActive(1L, instant)).thenReturn(instant.isBefore(fin) ? fin : null);
        return new VoyageurEligibilityService(voyageurs, restrictions, mock(TrajetRepository.class), Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test void detteEtSuspensionActivesBloquent() {
        var s = service(100, debut);
        assertThat(s.statut(1L).peutParticiper()).isFalse();
        assertThatThrownBy(() -> s.verifier(1L, false)).isInstanceOf(ParticipationException.class)
                .hasMessageContaining("est suspendu jusqu'au")
                .hasMessageContaining("trajet privé incomplet")
                .hasMessageContaining("ni créer ni rejoindre un trajet");
    }
    @Test void paiementAnticipeNeLevePasLaSuspension() {
        var s = service(0, debut.plusSeconds(3600));
        assertThat(s.statut(1L).finSuspension()).isEqualTo(fin);
        assertThatThrownBy(() -> s.verifier(1L, false)).hasMessageContaining("pénalité de 100 € est réglée")
                .hasMessageContaining("suspension reste toutefois active");
    }
    @Test void suspensionExpireeDetteImpayeeBloque() {
        var s = service(100, fin);
        assertThat(s.statut(1L).code()).isEqualTo("PENALITE_IMPAYEE");
        assertThatThrownBy(() -> s.verifier(1L, false)).hasMessageContaining("solde impayé");
    }
    @Test void dettePayeeEtSuspensionExpireeAutorise() {
        var s = service(0, fin);
        s.verifier(1L, false);
        assertThat(s.statut(1L).peutParticiper()).isTrue();
        assertThat(s.statut(1L).message()).isEqualTo("Vous pouvez participer aux trajets.");
    }
    @Test void justeAvantExpirationBloque() {
        var s = service(0, fin.minusNanos(1));
        assertThat(s.statut(1L).suspensionActive()).isTrue();
    }
    @Test void ajoutParTiersNeRevelePasLaDette() {
        var s = service(100, debut);
        assertThatThrownBy(() -> s.verifier(1L, true)).hasMessageContaining("Ce voyageur ne peut pas être ajouté au trajet")
                .hasMessageNotContaining("100").hasMessageNotContaining("solde");
    }
}
