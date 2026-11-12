package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Voyageur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoyageurRepository extends JpaRepository<Voyageur, Long> {
    Optional<Voyageur> findByMatricule(String matricule);
    Optional<Voyageur> findByMatriculeIgnoreCase(String matricule);
    boolean existsByMatricule(String matricule);
    boolean existsByEmail(String email);
}
