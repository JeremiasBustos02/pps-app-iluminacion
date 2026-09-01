package com.repository;

import com.entity.CuadrillaTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuadrillaTecnicoRepository extends JpaRepository<CuadrillaTecnico, Long> {
    List<CuadrillaTecnico> findByCuadrillaId(Long cuadrillaId);
    List<CuadrillaTecnico> findByUsuarioId(Long usuarioId);

    Optional<CuadrillaTecnico> findByCuadrillaIdAndUsuarioId(
            Long cuadrillaId,
            Long usuarioId
    );

    // Borrado directo (bulk) para reasignar la cuadrilla de un tecnico (RF-04)
    @Modifying
    @Query("DELETE FROM CuadrillaTecnico ct WHERE ct.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}