package com.repository;

import com.entity.CuadrillaTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
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
}