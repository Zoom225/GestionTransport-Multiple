package com.transport.gestiontransport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "penalites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restriction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    private Voyageur voyageur;

    @Column(nullable = false)
    private LocalDate dateFin;

    private LocalDate dateDebut;

    @Column(unique = true)
    private String sourceEvenement;

    @Column(nullable = false)
    private String motif;
}
