package com.transport.gestiontransport.entity;

import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "voyageurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voyageur extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String email;

    @Column
    private String passwordHash; // Added passwordHash field

    @Convert(converter = com.transport.gestiontransport.entity.converter.TypeVoyageurConverter.class)
    @Column(name = "type_voyageur", nullable = false)
    private TypeVoyageur typeVoyageur;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean peutProposerTrajet = false;

    @Column(nullable = false)
    private Double solde;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @OneToMany(mappedBy = "chauffeur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Trajet> trajetsOrganises = new HashSet<>();

    @OneToMany(mappedBy = "voyageur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();

    @OneToMany(mappedBy = "voyageur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Restriction> restrictions = new HashSet<>();
}
