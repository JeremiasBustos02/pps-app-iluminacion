package com.repository;

import com.entity.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.enums.EstadoReclamo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    Optional<Reclamo> findByNumeroSeguimiento(String numeroSeguimiento);
    List<Reclamo> findByUsuarioId(Long usuarioId);
    List<Reclamo> findByLuminariaId(Long luminariaId);
    List<Reclamo> findByEstado(String estado);

    @Query("SELECT r FROM Reclamo r LEFT JOIN r.luminaria l LEFT JOIN l.zona z WHERE "
            + "(:estado IS NULL OR r.estado = :estado) AND "
            + "(:zonaId IS NULL OR z.id = :zonaId) AND "
            + "(:tipoReclamoId IS NULL OR r.tipoReclamo.id = :tipoReclamoId)")
    List<Reclamo> filtrar(@Param("estado") EstadoReclamo estado,
                          @Param("zonaId") Long zonaId,
                          @Param("tipoReclamoId") Long tipoReclamoId);

}