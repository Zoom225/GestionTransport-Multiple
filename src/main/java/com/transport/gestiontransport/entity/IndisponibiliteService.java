package com.transport.gestiontransport.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "indisponibilites_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndisponibiliteService extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    private String raison;

    @Column(nullable = false)
    private Boolean global;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    @JsonBackReference("agence-indisponibilitesService")
    private Agence agence;
}
