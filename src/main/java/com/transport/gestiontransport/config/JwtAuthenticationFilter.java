package com.transport.gestiontransport.config;

import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtConfig.readVerifiedClaims(authHeader.substring(7)).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        if (subject.contains("@")) {
            administrateurRepository.findByEmail(subject)
                    .filter(admin -> admin.getTypeAdministrateur().name().equals(role))
                    .ifPresent(admin -> setAuthentication(subject, "ADMIN", role));
            return;
        }
        voyageurRepository.findByMatriculeIgnoreCase(subject)
                .filter(voyageur -> voyageur.getTypeVoyageur().name().equals(role))
                .ifPresent(voyageur -> setAuthentication(subject, "VOYAGEUR", role));
    }

    private void setAuthentication(String subject, String accountRole, String domainRole) {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + accountRole),
                new SimpleGrantedAuthority("ROLE_" + domainRole));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(subject, null, authorities));
    }
}
