package com.repository;

import com.entity.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.enums.EstadoReclamo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    Optional<Reclamo> findByNumeroSeguimiento(String numeroSeguimiento);
    List<Reclamo> findByUsuarioId(Long usuarioId);
    List<Reclamo> findByLuminariaIdOrderByFechaDesc(Long luminariaId);
    List<Reclamo> findByEstado(String estado);

    @Query("SELECT r FROM Reclamo r LEFT JOIN r.luminaria l LEFT JOIN l.zona z WHERE "
            + "(:estado IS NULL OR r.estado = :estado) AND "
            + "(:zonaId IS NULL OR z.id = :zonaId) AND "
            + "(:tipoReclamoId IS NULL OR r.tipoReclamo.id = :tipoReclamoId)")
    List<Reclamo> filtrar(@Param("estado") EstadoReclamo estado,
                          @Param("zonaId") Long zonaId,
                          @Param("tipoReclamoId") Long tipoReclamoId);

    // RF-05: reclamos activos de todas las luminarias en una sola consulta, para calcular el color del mapa
    @Query("SELECT r.luminaria.id AS luminariaId, r.estado AS estado, t.prioridad AS prioridad "
            + "FROM Reclamo r LEFT JOIN r.tipoReclamo t "
            + "WHERE r.luminaria IS NOT NULL AND r.estado IN :estados")
    List<ReclamoActivoView> findActivos(@Param("estados") List<EstadoReclamo> estados);

    List<Reclamo> findByLuminariaIdAndEstadoInOrderByFechaDesc(Long luminariaId, List<EstadoReclamo> estados);

    // RF-10: reclamos en cola de trabajo con prioridad igual o mayor a la indicada
    @Query("SELECT COUNT(r) FROM Reclamo r WHERE r.estado IN :estados AND r.tipoReclamo.prioridad >= :prioridad")
    long contarEnColaConPrioridadMinima(@Param("estados") List<EstadoReclamo> estados,
                                        @Param("prioridad") Integer prioridad);

    // RF-21: cantidad de reclamos por zona y estado en el período
    @Query("SELECT new com.repository.ReporteRows$ReclamosPorZonaEstado(z.nombre, r.estado, COUNT(r)) "
            + "FROM Reclamo r LEFT JOIN r.luminaria l LEFT JOIN l.zona z "
            + "WHERE r.fecha BETWEEN :desde AND :hasta GROUP BY z.nombre, r.estado")
    List<ReporteRows.ReclamosPorZonaEstado> contarPorZonaYEstado(@Param("desde") LocalDateTime desde,
                                                                  @Param("hasta") LocalDateTime hasta);

    // RF-08: correlativo real e incremental para el numeroSeguimiento (secuencia de la V3, antes sin usar)
    @Query(value = "SELECT nextval('reclamo_numero_seq')", nativeQuery = true)
    Long siguienteNumeroSeguimiento();

}