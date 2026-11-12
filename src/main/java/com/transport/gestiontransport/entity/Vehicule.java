package com.transport.gestiontransport.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "terrains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule extends BaseEntity {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = true)
    private Double prix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @JsonBackReference("agence-vehicules")
    private Agence agence;

    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("vehicule-trajets")
    private List<Trajet> trajets;

    public Double getPrix() {
        return prix;
    }
}
