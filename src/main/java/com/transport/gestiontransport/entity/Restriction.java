package com.transport.gestiontransport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "restrictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restriction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyageur_id", nullable = false)
    private Voyageur voyageur;

    @Column(nullable = false)
    private LocalDate dateFin;

    private LocalDate dateDebut;

    @Column(unique = true)
    private String sourceEvenement;

    @Column(nullable = false)
    private String motif;

    // Exact intervals for new suspensions. Legacy date-only restrictions stay unchanged.
    private java.time.Instant debutSuspension;
    private java.time.Instant finSuspension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_origine_id", unique = true)
    private Trajet trajetOrigine;

    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal montantPenalite;
}
