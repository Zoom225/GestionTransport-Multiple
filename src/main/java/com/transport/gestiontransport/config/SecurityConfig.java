package com.transport.gestiontransport.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/agences/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicules/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/voyageurs/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trajets/public", "/api/trajets/villes").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/trajets").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/trajets/organisateur/**").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.GET, "/api/trajets").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/trajets/agence/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/trajets/*").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/trajets/*").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/trajets/*/cancel").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/trajets/*/convert-public").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reservations/voyageur/**").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/trajet/**").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/absence").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/cancel").hasRole("VOYAGEUR")

                        .requestMatchers("/api/paiements/**").hasRole("VOYAGEUR")

                        .requestMatchers(HttpMethod.GET, "/api/voyageurs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/me").hasRole("VOYAGEUR")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/matricule/**").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/*/restrictions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/*/restriction").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/*/balance").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/voyageurs/*").hasAnyRole("VOYAGEUR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/voyageurs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/voyageurs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/voyageurs/**").hasRole("ADMIN")
                        .requestMatchers("/api/indisponibilites-service/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
