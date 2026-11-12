package com.transport.gestiontransport.service.impl;

import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.exception.ResourceNotFoundException;
import com.transport.gestiontransport.repository.AgenceRepository;
import com.transport.gestiontransport.service.AgenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenceServiceImpl implements AgenceService {

    private final AgenceRepository agenceRepository;

    @Override
    public Agence create(Agence agence) {
        return agenceRepository.save(agence);
    }

    @Override
    public Agence getById(Long id) {
        return agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agence introuvable avec l'ID : " + id));
    }

    @Override
    public List<Agence> getAll() {
        // C'est maintenant la bonne approche car les collections sont en LAZY
        // et ne seront pas chargées, évitant ainsi l'erreur 500.
        return agenceRepository.findAll();
    }

    @Override
    public Agence update(Long id, Agence agence) {
        Agence existing = getById(id);
        existing.setNom(agence.getNom());
        existing.setAdresse(agence.getAdresse());
        existing.setHeureOuverture(agence.getHeureOuverture());
        existing.setHeureFermeture(agence.getHeureFermeture());
        existing.setDureeTrajetMinutes(agence.getDureeTrajetMinutes());
        existing.setDureeEntreTrajetMinutes(agence.getDureeEntreTrajetMinutes());
        existing.setAnneeCivile(agence.getAnneeCivile());
        return agenceRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Agence existing = getById(id);
        agenceRepository.delete(existing);
    }
}
