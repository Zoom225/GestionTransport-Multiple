package com.transport.gestiontransport.config;

import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final AdministrateurRepository administrateurRepository;
    private final VoyageurRepository voyageurRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // si pas de token → on laisse passer (les routes publiques sont gérées dans SecurityConfig)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // enlève "Bearer "

        if (!jwtConfig.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = jwtConfig.extractEmail(token); // peut être un email (Admin) ou un matricule (Voyageur)
        String role = jwtConfig.extractRole(token);

        // Distinguer admin et voyageur par le format du subject
        boolean isAdmin = subject.contains("@"); // email = admin, matricule = voyageur

        if (isAdmin) {
            Administrateur admin = administrateurRepository.findByEmail(subject).orElse(null);
            if (admin != null) {
                authenticate(subject, role, true);
                log.debug("Admin {} authenticated with role {}", subject, role);
            }
        } else {
            Voyageur voyageur = voyageurRepository.findByMatriculeIgnoreCase(subject).orElse(null);
            if (voyageur != null) {
                authenticate(subject, role, false);
                log.debug("Voyageur {} authenticated with role {}", subject, role);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String principal, String role, boolean admin) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        authorities.add(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_VOYAGEUR"));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
