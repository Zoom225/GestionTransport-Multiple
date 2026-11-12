package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.VehiculeRepository;
import com.transport.gestiontransport.service.AgenceService;
import com.transport.gestiontransport.service.VehiculeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final AgenceService agenceService;

    @Override
    public Vehicule create(Vehicule vehicule, Long agenceId) {
        Agence agence = agenceService.getById(agenceId);
        vehicule.setAgence(agence);
        log.info("Vehicule créé pour cette agence {}", agenceId);
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule getById(Long id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule introuvable avec l'ID : " + id));
    }

    @Override
    public List<Vehicule> getAll() {
        return vehiculeRepository.findAll();
    }

    @Override
    public List<Vehicule> getByAgenceId(Long agenceId) {
        agenceService.getById(agenceId);
        return vehiculeRepository.findByAgenceId(agenceId);
    }

    @Override
    public Vehicule update(Long id, Vehicule vehicule) {
        Vehicule existing = getById(id);
        existing.setNom(vehicule.getNom());
        return vehiculeRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Vehicule existing = getById(id);
        vehiculeRepository.delete(existing);
    }
}
