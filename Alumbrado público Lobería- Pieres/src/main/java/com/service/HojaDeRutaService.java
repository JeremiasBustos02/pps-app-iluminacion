package com.service;

import com.dto.HojaDelDiaDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.entity.CuadrillaTecnico;
import com.entity.HojaDeRuta;
import com.entity.HojaDeRutaReclamo;
import com.entity.Reclamo;
import com.enums.EstadoReclamo;
import com.repository.CuadrillaTecnicoRepository;
import com.repository.HojaDeRutaReclamoRepository;
import com.repository.HojaDeRutaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class HojaDeRutaService {

    @Autowired
    private HojaDeRutaRepository hojaDeRutaRepository;
    @Autowired
    private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;
    @Autowired
    private HojaDeRutaReclamoRepository hojaDeRutaReclamoRepository;
    @Autowired
    private ReclamoService reclamoService;
    @Autowired
    private ReparacionService reparacionService;

    public List<HojaDeRuta> findAll() {
        return hojaDeRutaRepository.findAll();
    }

    public Optional<HojaDeRuta> findById(Long id) {
        return hojaDeRutaRepository.findById(id);
    }

    public HojaDeRuta save(HojaDeRuta hojaDeRuta) {
        return hojaDeRutaRepository.save(hojaDeRuta);
    }

    public void delete(Long id) {
        hojaDeRutaRepository.deleteById(id);
    }

    public List<HojaDeRuta> findMiHojaDelDia(Long usuarioId) {
        Long cuadrillaId = cuadrillaDelTecnico(usuarioId);

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        return hojaDeRutaRepository.findByCuadrillaIdAndFechaBetween(cuadrillaId, inicioDia, finDia);
    }

    // RF-20: hojas del día con sus reclamos, de mayor a menor prioridad (RF-09)
    @Transactional(readOnly = true)
    public List<HojaDelDiaDTO> getMiHojaDelDia(Long usuarioId) {
        List<HojaDelDiaDTO> resultado = new ArrayList<>();
        for (HojaDeRuta hoja : findMiHojaDelDia(usuarioId)) {
            HojaDelDiaDTO dto = new HojaDelDiaDTO();
            dto.setId(hoja.getId());
            dto.setFecha(hoja.getFecha());
            dto.setCuadrillaId(hoja.getCuadrilla().getId());
            dto.setCuadrilla(hoja.getCuadrilla().getNombre());
            dto.setReclamos(hojaDeRutaReclamoRepository.findByHojaDeRutaId(hoja.getId()).stream()
                    .map(HojaDeRutaReclamo::getReclamo)
                    .sorted(Comparator.comparing(
                                    (Reclamo r) -> r.getTipoReclamo() != null && r.getTipoReclamo().getPrioridad() != null
                                            ? r.getTipoReclamo().getPrioridad() : 0)
                            .reversed()
                            .thenComparing(Reclamo::getFecha, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toReclamoEnHoja)
                    .toList());
            resultado.add(dto);
        }
        return resultado;
    }

    // RF-20: al atender un reclamo de su hoja del día, el técnico registra el diagnóstico (RF-13),
    // sus observaciones (RF-14) y los materiales usados (RF-15); según el stock (RF-18) el reclamo
    // queda resuelto o en espera de material
    @Transactional
    public ReparacionResponseDTO atenderReclamo(Long usuarioId, Long reclamoId, ReparacionDTO diagnostico) {
        if (diagnostico.getObservacion() == null || diagnostico.getObservacion().isBlank()) {
            throw new IllegalArgumentException("Debe cargar las observaciones del trabajo realizado");
        }

        boolean enHojaDelDia = findMiHojaDelDia(usuarioId).stream()
                .anyMatch(hoja -> hojaDeRutaReclamoRepository
                        .findByHojaDeRutaIdAndReclamoId(hoja.getId(), reclamoId).isPresent());
        if (!enHojaDelDia) {
            throw new IllegalArgumentException("El reclamo no está en la hoja de ruta del día de su cuadrilla");
        }

        Reclamo reclamo = reclamoService.findById(reclamoId)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + reclamoId));
        // Un reclamo que sigue pendiente pasa a asignado al ser atendido por la cuadrilla
        if (reclamo.getEstado() == EstadoReclamo.PENDIENTE) {
            reclamoService.updateEstado(reclamoId, EstadoReclamo.ASIGNADO, "Atendido desde la hoja de ruta");
        }

        diagnostico.setReclamoId(reclamoId);
        List<Long> tecnicos = new ArrayList<>(diagnostico.getTecnicosIds());
        if (!tecnicos.contains(usuarioId)) {
            tecnicos.add(usuarioId);
        }
        diagnostico.setTecnicosIds(tecnicos);

        return reparacionService.registrarReparacion(diagnostico);
    }

    private Long cuadrillaDelTecnico(Long usuarioId) {
        CuadrillaTecnico ct = cuadrillaTecnicoRepository.findByUsuarioId(usuarioId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El técnico no pertenece a ninguna cuadrilla"));
        return ct.getCuadrilla().getId();
    }

    private HojaDelDiaDTO.ReclamoEnHojaInfo toReclamoEnHoja(Reclamo reclamo) {
        HojaDelDiaDTO.ReclamoEnHojaInfo info = new HojaDelDiaDTO.ReclamoEnHojaInfo();
        info.setId(reclamo.getId());
        info.setNumeroSeguimiento(reclamo.getNumeroSeguimiento());
        info.setEstado(reclamo.getEstado());
        info.setObservacionVecino(reclamo.getObservacion());
        info.setFechaLimite(reclamo.getFechaLimite());
        info.setPendienteDeAtencion(reclamo.getEstado() != null && reclamo.getEstado().esActivo());
        if (reclamo.getTipoReclamo() != null) {
            info.setTipoReclamo(reclamo.getTipoReclamo().getNombre());
            info.setPrioridad(reclamo.getTipoReclamo().getPrioridad());
        }
        if (reclamo.getLuminaria() != null) {
            info.setLuminariaId(reclamo.getLuminaria().getId());
            if (reclamo.getLuminaria().getZona() != null) {
                info.setZona(reclamo.getLuminaria().getZona().getNombre());
            }
        }
        return info;
    }
}
