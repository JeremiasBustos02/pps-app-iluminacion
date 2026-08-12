package com.service;

import com.dto.ReclamoDTO;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.entity.TipoReclamo;
import com.entity.Usuario;
import com.repository.LuminariaRepository;
import com.repository.ReclamoRepository;
import com.repository.TipoReclamoRepository;
import com.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class ReclamoService {

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private LuminariaRepository luminariaRepository;

    @Autowired
    private TipoReclamoRepository tipoReclamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    public Reclamo saveFromDTO(ReclamoDTO dto) {
        Reclamo reclamo = new Reclamo();

        // 1. Asignar Luminaria
        if (dto.getLuminariaId() != null) {
            Luminaria luminaria = luminariaRepository.findById(dto.getLuminariaId())
                    .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + dto.getLuminariaId()));
            reclamo.setLuminaria(luminaria);
        }

        // 2. Asignar TipoReclamo
        if (dto.getTipoReclamoId() != null) {
            TipoReclamo tipoReclamo = tipoReclamoRepository.findById(dto.getTipoReclamoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de reclamo no encontrado: " + dto.getTipoReclamoId()));
            reclamo.setTipoReclamo(tipoReclamo);
        }

        // 3. Asignar Usuario
        if (dto.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));
            reclamo.setUsuario(usuario);
        }

        // Generar número de seguimiento formato REC-YYYY-[timestamp]
        String anioActual = String.valueOf(Year.now().getValue());
        long correlativo = System.currentTimeMillis() % 10000;
        reclamo.setNumeroSeguimiento(String.format("REC-%s-%04d", anioActual, correlativo));

        // Valores por defecto
        reclamo.setEstado("PENDIENTE");
        reclamo.setFecha(LocalDateTime.now());

        return reclamoRepository.save(reclamo);
    }

    public Reclamo updateEstado(Long id, String nuevoEstado) {
        Reclamo reclamo = findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));
        reclamo.setEstado(nuevoEstado);
        return reclamoRepository.save(reclamo);
    }

    public void delete(Long id) {
        reclamoRepository.deleteById(id);
    }
}