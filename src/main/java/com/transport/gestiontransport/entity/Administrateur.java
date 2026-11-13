package com.transport.gestiontransport.entity;


import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "administrateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Administrateur extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Convert(converter = com.transport.gestiontransport.entity.converter.TypeAdministrateurConverter.class)
    @Column(nullable = false)
    private TypeAdministrateur typeAdministrateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;
}
