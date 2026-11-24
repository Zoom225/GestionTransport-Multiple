package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.impl.TrajetServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaliteOrganisateurTest {
    @Mock TrajetRepository trajets;
    @Mock VoyageurRepository voyageurs;
    @Mock EntityManager entityManager;
    @Mock RestrictionRepository restrictionRepository;
    @InjectMocks TrajetServiceImpl service;
    Voyageur organisateur;
    Voyageur chauffeur;
    Trajet trajet;

    @BeforeEach void preparer() {
        organisateur = Voyageur.builder().solde(25.0).build();
        organisateur.setId(1L);
        chauffeur = Voyageur.builder().solde(0.0).build();
        chauffeur.setId(2L);
        trajet = Trajet.builder().organisateur(organisateur).chauffeur(chauffeur)
                .typeTrajet(TypeTrajet.PRIVE).statut(StatutTrajet.PLANIFIE)
                .dateDebut(LocalDateTime.now().plusHours(12)).nbPassagersActuels(2).build();
        trajet.setId(10L);
        when(trajets.findById(10L)).thenReturn(Optional.of(trajet));
        lenient().when(voyageurs.findByIdForUpdate(1L)).thenReturn(Optional.of(organisateur));
    }

    @Test void factureCentEurosAOrganisateurSansEffacerDetteExistanteNiDebiterChauffeur() {
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        assertThat(organisateur.getSolde()).isEqualTo(125.0);
        assertThat(chauffeur.getSolde()).isZero();
        assertThat(trajet.getPenaliteIncompletImputeeLe()).isNotNull();
        verify(voyageurs, times(1)).save(organisateur);
    }

    @Test void sanctionAtomiqueContientOrigineMontantEtExactement168Heures() {
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        var capture = ArgumentCaptor.forClass(Restriction.class);
        verify(restrictionRepository).save(capture.capture());
        Restriction restriction = capture.getValue();
        assertThat(restriction.getVoyageur()).isSameAs(organisateur);
        assertThat(restriction.getTrajetOrigine()).isSameAs(trajet);
        assertThat(restriction.getMontantPenalite()).isEqualByComparingTo("100.00");
        assertThat(java.time.Duration.between(restriction.getDebutSuspension(), restriction.getFinSuspension()))
                .isEqualTo(java.time.Duration.ofHours(168));
        assertThat(restriction.getSourceEvenement()).isEqualTo("trajet-prive-incomplet:10");
    }

    @Test void ancienTrajetSansOrganisateurNeDebiteJamaisLeChauffeur() {
        trajet.setOrganisateur(null);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        assertThat(chauffeur.getSolde()).isZero();
        assertThat(trajet.getPenaliteIncompletImputeeLe()).isNull();
        verifyNoInteractions(voyageurs);
    }

    @Test void paiementSimuleRetireCentEurosUneSeuleFoisSansEffacerAutreDette() {
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        service.payerPenalite(10L, organisateur.getId());
        service.payerPenalite(10L, organisateur.getId());
        assertThat(organisateur.getSolde()).isEqualTo(25.0);
        assertThat(chauffeur.getSolde()).isZero();
        assertThat(trajet.getPenaliteIncompletPayeeLe()).isNotNull();
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        assertThat(organisateur.getSolde()).isEqualTo(25.0);
    }

    @Test void chauffeurNePeutPasPayerLaPenaliteDUnAutreVoyageur() {
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        assertThatThrownBy(() -> service.payerPenalite(10L, chauffeur.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(organisateur.getSolde()).isEqualTo(125.0);
    }

    @Test void neFactureNiTrajetCompletNiPublicNiAnnuleNiTropTotNiPasseNiPublicationConsentante() {
        trajet.setNbPassagersActuels(4);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        trajet.setNbPassagersActuels(0);
        trajet.setTypeTrajet(TypeTrajet.PUBLIC);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        trajet.setTypeTrajet(TypeTrajet.PRIVE);
        trajet.setStatut(StatutTrajet.ANNULE);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        trajet.setStatut(StatutTrajet.PLANIFIE);
        trajet.setDateDebut(LocalDateTime.now().plusHours(25));
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        trajet.setDateDebut(LocalDateTime.now().minusHours(1));
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        trajet.setDateDebut(LocalDateTime.now().plusHours(12));
        trajet.setPublicationAutomatiqueAutorisee(true);
        service.imputerPenaliteTrajetPriveIncomplet(10L);
        verifyNoInteractions(voyageurs);
    }
}
