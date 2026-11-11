package com.transport.gestiontransport.service;

import com.transport.gestiontransport.entity.Agence;

import java.util.List;

public interface AgenceService {
    Agence create(Agence agence);
    Agence getById(Long id);
    List<Agence> getAll();
    Agence update(Long id, Agence agence);
    void delete(Long id);
}
