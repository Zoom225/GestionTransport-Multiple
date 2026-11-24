package com.transport.gestiontransport.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    private static final String TEST_KEY = "TEST-KEY-A7c2Q4dV5fN9mR1k3sXT6uU8wY0bE";
    private static final String ROTATED_TEST_KEY = "ROTATED-TEST-KEY-Y2vN3rA0w4fX7dK8m9G1sH";

    @Test
    void refuseDeDemarrerAvecUneCleCourteOuRepetitive() {
        for (String invalid : new String[]{"trop-courte", "abcd".repeat(20)}) {
            new ApplicationContextRunner()
                    .withUserConfiguration(JwtConfig.class)
                    .withPropertyValues("jwt.expiration=60000", "jwt.secret=" + invalid)
                    .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "JWT_SECRET doit contenir au moins 32 octets et 16 caractères distincts."));
        }
    }

    @Test
    void rejetteUnJetonDeLAncienneCleApresRotation() {
        JwtConfig avant = new JwtConfig(TEST_KEY, 60000);
        JwtConfig apres = new JwtConfig(ROTATED_TEST_KEY, 60000);
        String token = avant.generateToken("TEST-VOYAGEUR", "VIP");

        assertThat(avant.readVerifiedClaims(token)).isPresent();
        assertThat(apres.readVerifiedClaims(token)).isEmpty();
        assertThat(apres.readVerifiedClaims(apres.generateToken("TEST-VOYAGEUR", "VIP"))).isPresent();
    }

    @Test
    void rejetteLesJetonsInvalidesExpiresOuSansExpiration() {
        var key = Keys.hmacShaKeyFor(TEST_KEY.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().subject("TEST-VOYAGEUR").claim("role", "VIP")
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        String sansExpiration = Jwts.builder().subject("TEST-VOYAGEUR").claim("role", "VIP")
                .signWith(key).compact();
        JwtConfig config = new JwtConfig(TEST_KEY, 60000);

        assertThat(config.readVerifiedClaims("format-invalide")).isEmpty();
        assertThat(config.readVerifiedClaims(expired)).isEmpty();
        assertThat(config.readVerifiedClaims(sansExpiration)).isEmpty();
    }
}
