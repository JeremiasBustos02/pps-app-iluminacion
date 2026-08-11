package com.repository;

import com.entity.CuadrillaTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuadrillaTecnicoRepository extends JpaRepository<CuadrillaTecnico, Long> {
    List<CuadrillaTecnico> findByCuadrillaId(Long cuadrillaId);
    List<CuadrillaTecnico> findByUsuarioId(Long usuarioId);
}