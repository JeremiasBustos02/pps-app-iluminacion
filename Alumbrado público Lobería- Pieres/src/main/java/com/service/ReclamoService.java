package com.service;

import com.dto.ReclamoDTO;
import com.entity.*;
import com.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.enums.EstadoReclamo;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ReclamoHistorialRepository reclamoHistorialRepository;

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
        reclamo.setEstado(EstadoReclamo.PENDIENTE);
        reclamo.setFecha(LocalDateTime.now());

        return reclamoRepository.save(reclamo);
    }

    @Transactional
    public Reclamo updateEstado(Long id, EstadoReclamo nuevoEstado, String observacion) {
        Reclamo reclamo = findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));

        EstadoReclamo estadoAnterior = reclamo.getEstado();

        if (estadoAnterior == nuevoEstado) {
            return reclamo;
        }

        if (!estadoAnterior.puedeTransicionarA(nuevoEstado)) {
            throw new RuntimeException(
                    "No se puede pasar de " + estadoAnterior + " a " + nuevoEstado);
        }

        // RF-17 y RF-10 - Lógica de pausa de SLA
        if (nuevoEstado == EstadoReclamo.ESPERA_EDEA) {
            // Logica de espera EDEA
        }

        // 1. Actualizar el estado en el reclamo
        reclamo.setEstado(nuevoEstado);
        Reclamo reclamoGuardado = reclamoRepository.save(reclamo);

        // 2. Crear y guardar el registro en el historial
        ReclamoHistorial historial = new ReclamoHistorial();
        historial.setReclamo(reclamoGuardado);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setObservacion(observacion);

        reclamoHistorialRepository.save(historial);

        return reclamoGuardado;
    }

    public List<Reclamo> filtrar(EstadoReclamo estado, Long zonaId, Long tipoReclamoId) {
        return reclamoRepository.filtrar(estado, zonaId, tipoReclamoId);
    }

    public void delete(Long id) {
        reclamoRepository.deleteById(id);
    }
}