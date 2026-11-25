package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Agence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {
    // La méthode standard findAll() héritée de JpaRepository est maintenant sûre
    // à utiliser grâce au chargement LAZY des collections dans l'entité Agence.
    // Si un chargement spécifique est nécessaire à l'avenir, il faudra utiliser
    // @EntityGraph sur une nouvelle méthode avec un nom non conflictuel.
}
