package com.transport.gestiontransport.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agence extends BaseEntity {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private LocalTime heureOuverture;

    @Column(nullable = false)
    private LocalTime heureFermeture;

    @Column(name = "duree_trajet_minutes", nullable = false)
    private Integer dureeTrajetMinutes;

    @Column(name = "duree_entre_trajet_minutes", nullable = false)
    private Integer dureeEntreTrajetMinutes;

    @Column(nullable = false)
    private Integer anneeCivile;

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("agence-vehicules")
    @Builder.Default
    private Set<Vehicule> vehicules = new HashSet<>();

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("agence-indisponibilitesService")
    @Builder.Default
    private Set<IndisponibiliteService> indisponibilitesService = new HashSet<>();
}
