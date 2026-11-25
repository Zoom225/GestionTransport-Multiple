package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Voyageur;

import java.util.List;

public interface VoyageurService {
    Voyageur create(Voyageur voyageur);
    Voyageur getById(Long id);
    Voyageur getByMatricule(String matricule);
    List<Voyageur> getAll();
    Voyageur update(Long id, Voyageur voyageur);
    void delete(Long id);
    boolean hasActiveRestriction(Long voyageurId);
    boolean hasOutstandingBalance(Long voyageurId);
    void addRestriction(Long voyageurId);
    void addRestrictionPourEvenement(Long voyageurId, String source, String motif);
    List<com.transport.gestiontransport.entity.Restriction> getRestrictions(Long voyageurId);
    Voyageur authenticate(String matricule, String password);
}
