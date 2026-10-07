package com.repository;

import com.entity.Reparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {
    List<Reparacion> findByReclamoIdOrderByFechaDesc(Long reclamoId);

    // RF-21: reparaciones realizadas en el período
    List<Reparacion> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    // RF-21: reparaciones por cuadrilla (según la cuadrilla actual de los técnicos que participaron)
    @Query("SELECT new com.repository.ReporteRows$ReparacionesPorCuadrilla(c.nombre, COUNT(DISTINCT rt.reparacion.id)) "
            + "FROM ReparacionTecnico rt, CuadrillaTecnico ct JOIN ct.cuadrilla c "
            + "WHERE ct.usuario = rt.usuario AND rt.reparacion.fecha BETWEEN :desde AND :hasta "
            + "GROUP BY c.nombre")
    List<ReporteRows.ReparacionesPorCuadrilla> contarPorCuadrilla(@Param("desde") LocalDateTime desde,
                                                                  @Param("hasta") LocalDateTime hasta);
}