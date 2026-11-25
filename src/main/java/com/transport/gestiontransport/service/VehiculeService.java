package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Vehicule;

import java.util.List;

public interface VehiculeService {
    Vehicule create(Vehicule vehicule, Long agenceId);
    Vehicule getById(Long id);
    List<Vehicule> getAll();
    List<Vehicule> getByAgenceId(Long agenceId);
    Vehicule update(Long id, Vehicule vehicule);
    void delete(Long id);
}
