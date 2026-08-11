package com.service;

import com.dto.ReclamoRequest;
import com.dto.ReclamoResponse;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.entity.TipoReclamo;
import com.repository.LuminariaRepository;
import com.repository.ReclamoRepository;
import com.repository.TipoReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private JdbcTemplate jdbcTemplate;

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

    public ReclamoResponse create(ReclamoRequest request) {
        Luminaria luminaria = luminariaRepository.findById(request.getLuminariaId())
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + request.getLuminariaId()));
        TipoReclamo tipoReclamo = tipoReclamoRepository.findById(request.getTipoReclamoId())
                .orElseThrow(() -> new RuntimeException("Tipo de reclamo no encontrado: " + request.getTipoReclamoId()));

        Reclamo reclamo = new Reclamo();
        reclamo.setLuminaria(luminaria);
        reclamo.setTipoReclamo(tipoReclamo);
        reclamo.setEstado("PENDIENTE");
        reclamo.setFecha(LocalDateTime.now());

        Long siguiente = jdbcTemplate.queryForObject("SELECT nextval('reclamo_numero_seq')", Long.class);
        reclamo.setNumeroSeguimiento("REC-" + Year.now().getValue() + "-" + String.format("%05d", siguiente));

        // tiempoEstimado y usuario quedan pendientes:
        // - usuario se completa cuando Auth esté mergeado (sale del token, no del request)
        // - tiempoEstimado placeholder simple según prioridad del tipo
        reclamo.setTiempoEstimado(calcularTiempoEstimado(tipoReclamo.getPrioridad()));

        Reclamo reclamoActual = reclamoRepository.save(reclamo);
        ReclamoResponse reclamoResponse = this.toResponse(reclamoActual);

        return reclamoResponse;
    }

    private Integer calcularTiempoEstimado(Integer prioridad) {
        if (prioridad == null) return null;
        return switch (prioridad) {
            case 3 -> 24;   // alta: 24hs
            case 2 -> 72;   // media: 3 días
            default -> 168; // baja: 7 días
        };
    }

    public Reclamo updateEstado(Long id, String nuevoEstado) {
        Reclamo reclamo = findById(id).orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));
        reclamo.setEstado(nuevoEstado);
        return reclamoRepository.save(reclamo);
    }

    private ReclamoResponse toResponse(Reclamo r) {
        ReclamoResponse dto = new ReclamoResponse();
        dto.setId(r.getId());
        dto.setNumeroSeguimiento(r.getNumeroSeguimiento());
        dto.setEstado(r.getEstado());
        dto.setFecha(r.getFecha());
        dto.setTiempoEstimado(r.getTiempoEstimado());
        dto.setLuminariaId(r.getLuminaria().getId());
        dto.setTipoReclamoId(r.getTipoReclamo().getId());
        dto.setTipoReclamoNombre(r.getTipoReclamo().getNombre());
        dto.setUsuarioId(r.getUsuario() != null ? r.getUsuario().getId() : null);
        return dto;
    }
}