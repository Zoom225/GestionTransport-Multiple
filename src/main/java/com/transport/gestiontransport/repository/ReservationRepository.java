package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByTrajetId(Long trajetId);
    List<Reservation> findByVoyageurId(Long voyageurId);
    boolean existsByTrajetIdAndVoyageurId(Long trajetId, Long voyageurId);
}
