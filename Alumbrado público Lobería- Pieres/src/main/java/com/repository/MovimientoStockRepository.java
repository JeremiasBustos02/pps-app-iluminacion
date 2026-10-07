package com.repository;

import com.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    List<MovimientoStock> findByMaterialId(Long materialId);
    List<MovimientoStock> findByReparacionId(Long reparacionId);

    // RF-21: unidades y monto por material y tipo de movimiento en el período
    @Query("SELECT new com.repository.ReporteRows$MovimientosPorMaterial("
            + "m.id, m.nombre, ms.tipo, SUM(ms.cantidad), SUM(ms.cantidad * ms.precioUnitario)) "
            + "FROM MovimientoStock ms JOIN ms.material m "
            + "WHERE ms.fecha BETWEEN :desde AND :hasta GROUP BY m.id, m.nombre, ms.tipo")
    List<ReporteRows.MovimientosPorMaterial> resumenPorMaterial(@Param("desde") LocalDateTime desde,
                                                                @Param("hasta") LocalDateTime hasta);
}