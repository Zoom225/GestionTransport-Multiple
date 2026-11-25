package com.transport.gestiontransport.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.transport.gestiontransport.entity.enums.StatutReservation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id", nullable = false)
    @JsonBackReference("trajet-reservations")
    private Trajet trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyageur_id", nullable = false)
    private Voyageur voyageur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("reservation-paiement")
    private Paiement paiement;

    private java.time.LocalDateTime absenceDeclareeLe;

    @Column(length = 500)
    private String motifAbsence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "absence_admin_id")
    private Administrateur absenceDeclareePar;
}
