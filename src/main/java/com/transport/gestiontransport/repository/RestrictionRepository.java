package com.transport.gestiontransport.repository;


import com.transport.gestiontransport.entity.Restriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RestrictionRepository extends JpaRepository<Restriction, Long> {
    boolean existsByVoyageurIdAndDateFinAfter(Long voyageurId, LocalDate date);
}
