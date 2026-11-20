package com.transport.gestiontransport.repository;


import com.transport.gestiontransport.entity.Restriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RestrictionRepository extends JpaRepository<Restriction, Long> {
    @org.springframework.data.jpa.repository.Query("select count(r) > 0 from Restriction r where r.voyageur.id = :voyageurId and r.finSuspension is null and r.dateFin > :date and (r.dateDebut is null or r.dateDebut <= :date)")
    boolean existsByVoyageurIdAndDateFinAfter(Long voyageurId, LocalDate date);
    boolean existsBySourceEvenement(String sourceEvenement);
    @org.springframework.data.jpa.repository.Query("select max(r.finSuspension) from Restriction r where r.voyageur.id = :id and r.debutSuspension <= :instant and r.finSuspension > :instant")
    java.time.Instant finSuspensionActive(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("instant") java.time.Instant instant);
    java.util.List<Restriction> findByVoyageurIdOrderByDateFinDescIdDesc(Long voyageurId);

    java.util.Optional<Restriction> findFirstByVoyageurIdAndDebutSuspensionLessThanEqualOrderByFinSuspensionDescIdDesc(
            Long voyageurId, java.time.Instant instant);
}
