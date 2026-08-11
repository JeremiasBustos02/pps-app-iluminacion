package com.service;

import com.entity.Reclamo;
import com.repository.ReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReclamoService {

    @Autowired
    private ReclamoRepository reclamoRepository;

    public List<Reclamo> findAll() {
        return reclamoRepository.findAll();
    }

    public Optional<Reclamo> findById(Long id) {
        return reclamoRepository.findById(id);
    }

    public Optional<Reclamo> findByNumeroSeguimiento(String numeroSeguimiento) {
        return reclamoRepository.findByNumeroSeguimiento(numeroSeguimiento);
    }

    public List<Reclamo> findByUsuario(Long usuarioId) {
        return reclamoRepository.findByUsuarioId(usuarioId);
    }

    public Reclamo save(Reclamo reclamo) {
        if (reclamo.getNumeroSeguimiento() == null || reclamo.getNumeroSeguimiento().isBlank()) {
            reclamo.setNumeroSeguimiento("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (reclamo.getEstado() == null) {
            reclamo.setEstado("PENDIENTE");
        }
        return reclamoRepository.save(reclamo);
    }

    public Reclamo updateEstado(Long id, String nuevoEstado) {
        Reclamo reclamo = findById(id).orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));
        reclamo.setEstado(nuevoEstado);
        return reclamoRepository.save(reclamo);
    }

    public void delete(Long id) {
        reclamoRepository.deleteById(id);
    }
}