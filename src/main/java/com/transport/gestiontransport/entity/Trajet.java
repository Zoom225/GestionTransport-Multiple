package com.transport.gestiontransport.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet extends BaseEntity {
    public static final int MAX_CLIENTS = 4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terrain_id", nullable = false)
    @JsonBackReference("vehicule-trajets")
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Voyageur chauffeur;

    @Enumerated(EnumType.STRING)
    private com.transport.gestiontransport.entity.enums.VilleBelge villeDepart;

    @Enumerated(EnumType.STRING)
    private com.transport.gestiontransport.entity.enums.VilleBelge villeArrivee;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean publicationAutomatiqueAutorisee = false;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_match", nullable = false)
    private TypeTrajet typeTrajet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTrajet statut;

    @Column(name = "nb_joueurs_actuels", nullable = false)
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(MAX_CLIENTS)
    private Integer nbPassagersActuels;

    @Column(nullable = false)
    private Double prixTotal;

    @Column(name = "prix_par_joueur", nullable = false)
    private Double prixParPassager;

    private LocalDateTime dateConversionPublic;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("trajet-reservations")
    private List<Reservation> reservations;

    // L'entité est maintenant un simple POJO qui représente la table de la base de données.
    // Toute la logique de manipulation des dates a été déplacée dans la couche service.
}
