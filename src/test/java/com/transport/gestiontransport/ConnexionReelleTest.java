package com.transport.gestiontransport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConnexionReelleTest {
    private static final String ADMIN = "administration-validation@example.test";
    private static final String VOYAGEUR = "VALIDATION-CONNEXION";
    private static final String MOT_DE_PASSE = "Fixture-Connexion-2026";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AdministrateurRepository administrateurs;
    @Autowired
    VoyageurRepository voyageurs;

    @BeforeEach
    void creerLesComptesDeTest() {
        String hash = passwordEncoder.encode(MOT_DE_PASSE);
        administrateurs.saveAndFlush(Administrateur.builder()
                .matricule("ADMIN-VALIDATION").nom("Validation").prenom("Administration")
                .email(ADMIN).typeAdministrateur(TypeAdministrateur.GLOBAL).passwordHash(hash).build());
        voyageurs.saveAndFlush(Voyageur.builder()
                .matricule(VOYAGEUR).nom("Validation").prenom("Voyageur")
                .solde(BigDecimal.ZERO).typeVoyageur(TypeVoyageur.VIP).passwordHash(hash).build());
    }

    @Test
    void connexionAdministrateurVerifieBcryptEtDonneLePerimetreAdministrateur() throws Exception {
        connexion("/api/auth/login", "email", ADMIN, "mot-invalide")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.token").doesNotExist());
        String jeton = jeton(connexion("/api/auth/login", "email", ADMIN, MOT_DE_PASSE)
                .andExpect(jsonPath("$.role").value("GLOBAL")));

        mvc.perform(get("/api/voyageurs").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk());
        mvc.perform(get("/api/voyageurs/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isForbidden());
    }

    @Test
    void connexionVoyageurVerifieBcryptEtDonneAccesAuProfilPersonnel() throws Exception {
        connexion("/api/voyageurs/login", "matricule", VOYAGEUR, "mot-invalide")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.token").doesNotExist());
        String jeton = jeton(connexion("/api/voyageurs/login", "matricule", VOYAGEUR, MOT_DE_PASSE)
                .andExpect(jsonPath("$.matricule").value(VOYAGEUR)));

        mvc.perform(get("/api/voyageurs/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$.matricule").value(VOYAGEUR));
        mvc.perform(get("/api/voyageurs").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isForbidden());
    }

    private ResultActions connexion(String route, String champ, String identifiant, String motDePasse) throws Exception {
        return mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(champ, identifiant, "password", motDePasse))));
    }

    private String jeton(ResultActions reponse) throws Exception {
        byte[] json = reponse.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readTree(json).get("token").asText();
    }
}
