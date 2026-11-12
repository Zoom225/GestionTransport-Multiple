package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.VoyageurRepository;
import com.transport.gestiontransport.repository.RestrictionRepository;
import com.transport.gestiontransport.service.impl.VoyageurServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder; // Added import

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de VoyageurService")
class VoyageurServiceTest {

    @Mock
    private VoyageurRepository voyageurRepository;

    @Mock
    private RestrictionRepository restrictionRepository;

    @Mock // Added PasswordEncoder mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private VoyageurServiceImpl voyageurService;

    private Voyageur voyageurGlobal;
    private Voyageur voyageurAgence;
    private Voyageur voyageurLibre;
    private Agence agence;

    @BeforeEach
    void setUp() {
        agence = Agence.builder()
                .nom("Agence Lyon")
                .build();
        agence.setId(1L); // pas de setter via Builder car id vient de BaseEntity

        voyageurGlobal = Voyageur.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas@email.com")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();

        voyageurAgence = Voyageur.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom@email.com")
                .typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0)
                .agence(agence)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();

        voyageurLibre = Voyageur.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex@email.com")
                .typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0)
                .passwordHash("$2a$10$somehashedpassword") // Added a password hash for tests
                .build();
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ doit créer un voyageur GLOBAL avec un matricule valide G1001")
        void shouldCreateGlobalVoyageur() {
            when(voyageurRepository.existsByMatricule("G1001")).thenReturn(false);
            when(voyageurRepository.existsByEmail(anyString())).thenReturn(false);
            when(voyageurRepository.save(any())).thenReturn(voyageurGlobal);

            Voyageur result = voyageurService.create(voyageurGlobal);

            assertThat(result).isNotNull();
            assertThat(result.getMatricule()).isEqualTo("G1001");
            assertThat(result.getTypeVoyageur()).isEqualTo(TypeVoyageur.VIP);
            assertThat(result.getSolde()).isEqualTo(0.0);
            verify(voyageurRepository, times(1)).save(voyageurGlobal);
        }

        @Test
        @DisplayName("âœ… doit normaliser le matricule en majuscules lors de la création")
        void shouldNormalizeMatriculeOnCreate() {
            Voyageur voyageur = Voyageur.builder()
                    .matricule("g1001")
                    .nom("Martin")
                    .prenom("Lucas")
                    .email("lucas@email.com")
                    .typeVoyageur(TypeVoyageur.VIP)
                    .solde(0.0)
                    .passwordHash("$2a$10$somehashedpassword")
                    .build();

            when(voyageurRepository.existsByMatricule("G1001")).thenReturn(false);
            when(voyageurRepository.existsByEmail(anyString())).thenReturn(false);
            when(voyageurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Voyageur result = voyageurService.create(voyageur);

            assertThat(result.getMatricule()).isEqualTo("G1001");
            verify(voyageurRepository).existsByMatricule("G1001");
            verify(voyageurRepository).save(argThat(saved -> "G1001".equals(saved.getMatricule())));
        }

        @Test
        @DisplayName("✅ doit créer un voyageur AGENCE lié à une agence")
        void shouldCreateAgenceVoyageur() {
            when(voyageurRepository.existsByMatricule("S10001")).thenReturn(false);
            when(voyageurRepository.existsByEmail(anyString())).thenReturn(false);
            when(voyageurRepository.save(any())).thenReturn(voyageurAgence);

            Voyageur result = voyageurService.create(voyageurAgence);

            assertThat(result.getTypeVoyageur()).isEqualTo(TypeVoyageur.LOCAL);
            assertThat(result.getAgence()).isNotNull();
            assertThat(result.getAgence().getNom()).isEqualTo("Agence Lyon");
        }

        @Test
        @DisplayName("✅ doit créer un voyageur LIBRE avec un matricule valide L10001")
        void shouldCreateLibreVoyageur() {
            when(voyageurRepository.existsByMatricule("L10001")).thenReturn(false);
            when(voyageurRepository.existsByEmail(anyString())).thenReturn(false);
            when(voyageurRepository.save(any())).thenReturn(voyageurLibre);

            Voyageur result = voyageurService.create(voyageurLibre);

            assertThat(result.getTypeVoyageur()).isEqualTo(TypeVoyageur.LIBRE);
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le matricule existe déjà")
        void shouldThrowWhenMatriculeExists() {
            when(voyageurRepository.existsByMatricule("G1001")).thenReturn(true);

            assertThatThrownBy(() -> voyageurService.create(voyageurGlobal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("matricule existe déjà");

            verify(voyageurRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand l'email existe déjà")
        void shouldThrowWhenEmailExists() {
            when(voyageurRepository.existsByMatricule("G1001")).thenReturn(false);
            when(voyageurRepository.existsByEmail("lucas@email.com")).thenReturn(true);

            assertThatThrownBy(() -> voyageurService.create(voyageurGlobal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email existe déjà");

            verify(voyageurRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand un voyageur AGENCE n'a pas de agence")
        void shouldThrowWhenAgenceVoyageurHasNoAgence() {
            Voyageur voyageurSansAgence = Voyageur.builder()
                    .matricule("S10002")
                    .nom("Test")
                    .prenom("Test")
                    .typeVoyageur(TypeVoyageur.LOCAL)
                    .solde(0.0)
                    .agence(null) // ← pas de agence
                    .build();

            when(voyageurRepository.existsByMatricule("S10002")).thenReturn(false);

            assertThatThrownBy(() -> voyageurService.create(voyageurSansAgence))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("voyageur AGENCE doit être lié");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le format du matricule GLOBAL est incorrect")
        void shouldThrowWhenGlobalMatriculeFormatIsWrong() {
            Voyageur badVoyageur = Voyageur.builder()
                    .matricule("G12") // ← trop court, doit être G + 4 chiffres
                    .typeVoyageur(TypeVoyageur.VIP)
                    .solde(0.0)
                    .build();

            assertThatThrownBy(() -> voyageurService.create(badVoyageur))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le format du matricule AGENCE est incorrect")
        void shouldThrowWhenAgenceMatriculeFormatIsWrong() {
            Voyageur badVoyageur = Voyageur.builder()
                    .matricule("S123") // ← doit être S + 5 chiffres
                    .typeVoyageur(TypeVoyageur.LOCAL)
                    .solde(0.0)
                    .agence(agence)
                    .build();

            assertThatThrownBy(() -> voyageurService.create(badVoyageur))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("❌ doit lever une BusinessException quand le matricule LIBRE commence par la mauvaise lettre")
        void shouldThrowWhenLibreMatriculeStartsWithWrongLetter() {
            Voyageur badVoyageur = Voyageur.builder()
                    .matricule("X10001") // ← doit commencer par L
                    .typeVoyageur(TypeVoyageur.LIBRE)
                    .solde(0.0)
                    .build();

            assertThatThrownBy(() -> voyageurService.create(badVoyageur))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Format de matricule invalide");
        }

        @Test
        @DisplayName("✅ doit initialiser le solde à 0.0 lors de la création")
        void shouldInitializeSoldeToZero() {
            voyageurGlobal.setSolde(99.0); // on force un solde non nul
            when(voyageurRepository.existsByMatricule(any())).thenReturn(false);
            when(voyageurRepository.existsByEmail(any())).thenReturn(false);
            when(voyageurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Voyageur result = voyageurService.create(voyageurGlobal);

            assertThat(result.getSolde()).isEqualTo(0.0);
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById() and getByMatricule()")
    class GetTests {

        @Test
        @DisplayName("✅ doit retourner le voyageur quand l'id existe")
        void shouldReturnVoyageurById() {
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));

            Voyageur result = voyageurService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getMatricule()).isEqualTo("G1001");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand l'id n'est pas trouvé")
        void shouldThrowWhenIdNotFound() {
            when(voyageurRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voyageurService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Voyageur introuvable");
        }

        @Test
        @DisplayName("✅ doit retourner le voyageur quand le matricule existe")
        void shouldReturnVoyageurByMatricule() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));

            Voyageur result = voyageurService.getByMatricule("G1001");

            assertThat(result.getMatricule()).isEqualTo("G1001");
        }

        @Test
        @DisplayName("âœ… doit normaliser le matricule avant la recherche")
        void shouldNormalizeMatriculeBeforeLookup() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));

            Voyageur result = voyageurService.getByMatricule("g1001");

            assertThat(result.getMatricule()).isEqualTo("G1001");
            verify(voyageurRepository).findByMatriculeIgnoreCase("G1001");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException quand le matricule n'est pas trouvé")
        void shouldThrowWhenMatriculeNotFound() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voyageurService.getByMatricule("G9999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("G9999");
        }
    }

    // ================================================================
    // AUTHENTICATE
    // ================================================================
    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        @Test
        @DisplayName("✅ doit retourner le voyageur quand les identifiants sont valides")
        void shouldAuthenticateWithValidCredentials() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));
            when(passwordEncoder.matches("Voyageur1234!", voyageurGlobal.getPasswordHash())).thenReturn(true);

            Voyageur result = voyageurService.authenticate("G1001", "Voyageur1234!");

            assertThat(result).isEqualTo(voyageurGlobal);
        }

        @Test
        @DisplayName("❌ doit refuser quand le mot de passe est invalide")
        void shouldRejectInvalidPassword() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));
            when(passwordEncoder.matches("wrong-password", voyageurGlobal.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> voyageurService.authenticate("G1001", "wrong-password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("❌ doit refuser quand le matricule est introuvable")
        void shouldRejectInvalidMatricule() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voyageurService.authenticate("G9999", "Voyageur1234!"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("❌ doit refuser quand le voyageur n'a pas de hash de mot de passe")
        void shouldRejectVoyageurWithoutPasswordHash() {
            voyageurGlobal.setPasswordHash(null); // Simulate old account without hash
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));

            assertThatThrownBy(() -> voyageurService.authenticate("G1001", "Voyageur1234!"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Identifiants invalides.");
        }

        @Test
        @DisplayName("✅ doit normaliser le matricule en majuscules avant la recherche")
        void shouldNormalizeMatriculeToUpperCase() {
            when(voyageurRepository.findByMatriculeIgnoreCase("G1001")).thenReturn(Optional.of(voyageurGlobal));
            when(passwordEncoder.matches("Voyageur1234!", voyageurGlobal.getPasswordHash())).thenReturn(true);

            voyageurService.authenticate("g1001", "Voyageur1234!");

            verify(voyageurRepository).findByMatriculeIgnoreCase("G1001");
        }
    }

    // ================================================================
    // PENALTY
    // ================================================================
    @Nested
    @DisplayName("hasActiveRestriction()")
    class RestrictionTests {

        @Test
        @DisplayName("✅ doit retourner vrai quand le voyageur a une restriction active")
        void shouldReturnTrueWhenActiveRestriction() {
            when(restrictionRepository.existsByVoyageurIdAndDateFinAfter(eq(1L), any(LocalDate.class)))
                    .thenReturn(true);

            boolean result = voyageurService.hasActiveRestriction(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("✅ doit retourner faux quand le voyageur n'a pas de restriction active")
        void shouldReturnFalseWhenNoRestriction() {
            when(restrictionRepository.existsByVoyageurIdAndDateFinAfter(eq(1L), any(LocalDate.class)))
                    .thenReturn(false);

            boolean result = voyageurService.hasActiveRestriction(1L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("✅ doit ajouter une restriction avec dateFin = aujourd'hui + 7 jours")
        void shouldAddRestrictionWithCorrectDateFin() {
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));
            when(restrictionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            voyageurService.addRestriction(1L);

            verify(restrictionRepository, times(1)).save(argThat(restriction ->
                    restriction.getDateFin().equals(LocalDate.now().plusWeeks(1)) &&
                            restriction.getMotif().equals("Trajet privé incomplet avant l'échéance")
            ));
        }
    }

    // ================================================================
    // OUTSTANDING BALANCE
    // ================================================================
    @Nested
    @DisplayName("hasOutstandingBalance()")
    class BalanceTests {

        @Test
        @DisplayName("✅ doit retourner vrai quand le voyageur a un solde impayé")
        void shouldReturnTrueWhenBalancePositive() {
            voyageurGlobal.setSolde(15.0);
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));

            boolean result = voyageurService.hasOutstandingBalance(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("✅ doit retourner faux quand le voyageur n'a pas de solde impayé")
        void shouldReturnFalseWhenBalanceZero() {
            voyageurGlobal.setSolde(0.0);
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));

            boolean result = voyageurService.hasOutstandingBalance(1L);

            assertThat(result).isFalse();
        }
    }

    // ================================================================
    // UPDATE
    // ================================================================
    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("✅ doit mettre à jour le nom et l'email du voyageur")
        void shouldUpdateVoyageur() {
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));
            when(voyageurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Voyageur updated = Voyageur.builder()
                    .nom("Nouveau")
                    .prenom("Nom")
                    .email("nouveau@email.com")
                    .build();

            Voyageur result = voyageurService.update(1L, updated);

            assertThat(result.getNom()).isEqualTo("Nouveau");
            assertThat(result.getPrenom()).isEqualTo("Nom");
            assertThat(result.getEmail()).isEqualTo("nouveau@email.com");
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException lors de la mise à jour d'un voyageur inexistant")
        void shouldThrowWhenUpdatingNonExistentVoyageur() {
            when(voyageurRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voyageurService.update(99L, voyageurGlobal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================================================================
    // DELETE
    // ================================================================
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("✅ doit supprimer le voyageur quand il existe")
        void shouldDeleteVoyageur() {
            when(voyageurRepository.findById(1L)).thenReturn(Optional.of(voyageurGlobal));

            voyageurService.delete(1L);

            verify(voyageurRepository, times(1)).delete(voyageurGlobal);
        }

        @Test
        @DisplayName("❌ doit lever une ResourceNotFoundException lors de la suppression d'un voyageur inexistant")
        void shouldThrowWhenDeletingNonExistentVoyageur() {
            when(voyageurRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voyageurService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(voyageurRepository, never()).delete(any());
        }
    }
}
