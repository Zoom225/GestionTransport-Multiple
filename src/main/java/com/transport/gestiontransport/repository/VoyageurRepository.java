package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Voyageur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoyageurRepository extends JpaRepository<Voyageur, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voyageur v where v.id = :id")
    Optional<Voyageur> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Voyageur> findByMatricule(String matricule);
    Optional<Voyageur> findByMatriculeIgnoreCase(String matricule);
    boolean existsByMatricule(String matricule);
    boolean existsByEmail(String email);
}
