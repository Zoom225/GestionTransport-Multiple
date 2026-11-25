package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.IndisponibiliteService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IndisponibiliteServiceRepository extends JpaRepository<IndisponibiliteService, Long> {

    boolean existsByDateAndGlobalTrue(LocalDate date);

    boolean existsByDateAndAgenceId(LocalDate date, Long agenceId);

    List<IndisponibiliteService> findByGlobalTrue();

    List<IndisponibiliteService> findByAgenceId(Long agenceId);
}
