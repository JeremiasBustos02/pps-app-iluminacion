package com.service;

import com.entity.Cuadrilla;
import com.repository.CuadrillaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CuadrillaService {

    @Autowired
    private CuadrillaRepository cuadrillaRepository;

    public List<Cuadrilla> findAll() {
        return cuadrillaRepository.findByDeletedAtIsNull();
    }

    public Optional<Cuadrilla> findById(Long id) {
        return cuadrillaRepository.findById(id).filter(c -> c.getDeletedAt() == null);
    }

    public Cuadrilla save(Cuadrilla cuadrilla) {
        return cuadrillaRepository.save(cuadrilla);
    }

    public void delete(Long id) {
        Cuadrilla cuadrilla = findById(id).orElseThrow(() -> new RuntimeException("Cuadrilla no encontrada: " + id));
        cuadrilla.setDeletedAt(LocalDateTime.now());
        cuadrillaRepository.save(cuadrilla);
    }

    public Cuadrilla update(Long id, Cuadrilla updatedCuadrilla) {
        Cuadrilla existingCuadrilla = findById(id)
                .orElseThrow(() -> new RuntimeException("Cuadrilla no encontrada: " + id));

        existingCuadrilla.setNombre(updatedCuadrilla.getNombre());

        return cuadrillaRepository.save(existingCuadrilla);
    }
}