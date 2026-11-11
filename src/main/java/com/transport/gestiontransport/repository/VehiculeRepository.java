package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Vehicule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
    @EntityGraph(attributePaths = "agence")
    List<Vehicule> findByAgenceId(Long agenceId);
}
