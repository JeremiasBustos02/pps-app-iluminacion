package com.service;

import com.entity.Componente;
import com.repository.ComponenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ComponenteService {

    @Autowired
    private ComponenteRepository componenteRepository;

    public List<Componente> findAll() {
        return componenteRepository.findByDeletedAtIsNull();
    }

    public Optional<Componente> findById(Long id) {
        return componenteRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Transactional
    public Componente save(Componente componente) {
        return componenteRepository.save(componente);
    }

    @Transactional
    public void delete(Long id) {
        Componente comp = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Componente no encontrado: " + id));
        comp.setDeletedAt(LocalDateTime.now());
        componenteRepository.save(comp);
    }
}