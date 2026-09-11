package com.service;

import com.dto.ReclamoDTO;
import com.entity.*;
import com.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.enums.EstadoReclamo;
import org.springframework.transaction.annotation.Transactional;
import com.dto.ReclamoPaqueteDTO;
import com.entity.ReclamoHistorial;

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
        // RF-07: el reclamo se genera seleccionando un punto en el mapa (luminaria)
        // y tipificando el problema a partir del catálogo predefinido; ambos son obligatorios.
        if (dto.getLuminariaId() == null) {
            throw new RuntimeException("Debe seleccionar una luminaria en el mapa");
        }
        if (dto.getTipoReclamoId() == null) {
            throw new RuntimeException("Debe seleccionar un tipo de problema");
        }

        Reclamo reclamo = new Reclamo();

        // 1. Asignar Luminaria (punto seleccionado en el mapa)
        Luminaria luminaria = luminariaRepository.findById(dto.getLuminariaId())
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + dto.getLuminariaId()));
        reclamo.setLuminaria(luminaria);

        // 2. Asignar TipoReclamo (tipificación del catálogo predefinido)
        TipoReclamo tipoReclamo = tipoReclamoRepository.findById(dto.getTipoReclamoId())
                .orElseThrow(() -> new RuntimeException("Tipo de reclamo no encontrado: " + dto.getTipoReclamoId()));
        reclamo.setTipoReclamo(tipoReclamo);

        // 3. Asignar Usuario
        if (dto.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));
            reclamo.setUsuario(usuario);
        }

        // RF-08: número de seguimiento único, formato REC-YYYY-NNNNN.
        // El correlativo sale de la secuencia de la base (reclamo_numero_seq), no del reloj:
        // así se garantiza unicidad real incluso con altas concurrentes.
        String anioActual = String.valueOf(Year.now().getValue());
        Long correlativo = reclamoRepository.siguienteNumeroSeguimiento();
        reclamo.setNumeroSeguimiento(String.format("REC-%s-%05d", anioActual, correlativo));

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

    public ReclamoPaqueteDTO getPaquete(Long id) {
        // 1. Buscar el reclamo
        Reclamo reclamo = reclamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));

        // 2. Buscar el historial
        List<ReclamoHistorial> historial = reclamoHistorialRepository
                .findByReclamoIdOrderByFechaCambioDesc(id);

        // 3. Crear el DTO y setear datos del reclamo
        ReclamoPaqueteDTO dto = new ReclamoPaqueteDTO();
        dto.setId(reclamo.getId());
        dto.setNumeroSeguimiento(reclamo.getNumeroSeguimiento());
        dto.setEstado(reclamo.getEstado());
        dto.setFecha(reclamo.getFecha());
        dto.setTiempoEstimado(reclamo.getTiempoEstimado());

        // 4. Bloque tipo de reclamo
        if (reclamo.getTipoReclamo() != null) {
            ReclamoPaqueteDTO.TipoReclamoInfo tr = new ReclamoPaqueteDTO.TipoReclamoInfo();
            tr.setId(reclamo.getTipoReclamo().getId());
            tr.setNombre(reclamo.getTipoReclamo().getNombre());
            tr.setPrioridad(reclamo.getTipoReclamo().getPrioridad());
            dto.setTipoReclamo(tr);
        }

        // 5. Bloque vecino
        if (reclamo.getUsuario() != null) {
            ReclamoPaqueteDTO.VecinoInfo v = new ReclamoPaqueteDTO.VecinoInfo();
            v.setId(reclamo.getUsuario().getId());
            v.setNombre(reclamo.getUsuario().getNombre());
            v.setDni(reclamo.getUsuario().getDni());
            v.setEmail(reclamo.getUsuario().getEmail());
            dto.setVecino(v);
        }

        // 6. Bloque luminaria
        if (reclamo.getLuminaria() != null) {
            ReclamoPaqueteDTO.LuminariaInfo l = new ReclamoPaqueteDTO.LuminariaInfo();
            l.setId(reclamo.getLuminaria().getId());
            l.setTipo(reclamo.getLuminaria().getTipo());
            l.setEstado(reclamo.getLuminaria().getEstado());
            if (reclamo.getLuminaria().getZona() != null) {
                l.setZona(reclamo.getLuminaria().getZona().getNombre());
            }
            dto.setLuminaria(l);
        }

        // 7. Bloque historial
        List<ReclamoPaqueteDTO.HistorialInfo> historialDTO = historial.stream()
                .map(h -> {
                    ReclamoPaqueteDTO.HistorialInfo hi = new ReclamoPaqueteDTO.HistorialInfo();
                    hi.setEstadoAnterior(h.getEstadoAnterior());
                    hi.setEstadoNuevo(h.getEstadoNuevo());
                    hi.setObservacion(h.getObservacion());
                    hi.setFechaCambio(h.getFechaCambio());
                    return hi;
                })
                .toList();
        dto.setHistorial(historialDTO);

        return dto;
    }

    public void delete(Long id) {
        reclamoRepository.deleteById(id);
    }
}