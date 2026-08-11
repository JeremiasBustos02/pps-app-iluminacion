package com.service;

import com.entity.Zona;
import com.repository.ZonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ZonaService {

    @Autowired
    private ZonaRepository zonaRepository;

    public List<Zona> findAll() {
        return zonaRepository.findByDeletedAtIsNull();
    }

    public Optional<Zona> findById(Long id) {
        return zonaRepository.findById(id).filter(z -> z.getDeletedAt() == null);
    }

    public Zona save(Zona zona) {
        return zonaRepository.save(zona);
    }

    public Zona update(Long id, Zona zonaDetails) {
        Zona zona = findById(id).orElseThrow(() -> new RuntimeException("Zona no encontrada: " + id));
        zona.setLocalidad(zonaDetails.getLocalidad());
        return zonaRepository.save(zona);
    }

    public void delete(Long id) {
        Zona zona = findById(id).orElseThrow(() -> new RuntimeException("Zona no encontrada: " + id));
        zona.setDeletedAt(LocalDateTime.now());
        zonaRepository.save(zona);
    }
}