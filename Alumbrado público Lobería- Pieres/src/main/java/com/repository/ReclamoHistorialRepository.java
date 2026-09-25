package com.repository;

import com.entity.ReclamoHistorial;
import com.enums.EstadoReclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReclamoHistorialRepository extends JpaRepository<ReclamoHistorial, Long> {
    // Te va a servir para mostrarle al administrador la línea de tiempo del reclamo
    List<ReclamoHistorial> findByReclamoIdOrderByFechaCambioDesc(Long reclamoId);

    // RF-21: cambios a un estado dado en el período, con su reclamo y tipo (tiempo de resolución)
    @Query("SELECT h FROM ReclamoHistorial h JOIN FETCH h.reclamo r LEFT JOIN FETCH r.tipoReclamo "
            + "WHERE h.estadoNuevo = :estado AND h.fechaCambio BETWEEN :desde AND :hasta")
    List<ReclamoHistorial> findCambiosAEstadoEntre(@Param("estado") EstadoReclamo estado,
                                                   @Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta);
}