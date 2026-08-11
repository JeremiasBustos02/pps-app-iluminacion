package com.repository;

import com.entity.ReparacionTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReparacionTecnicoRepository extends JpaRepository<ReparacionTecnico, Long> {
    List<ReparacionTecnico> findByReparacionId(Long reparacionId);
    List<ReparacionTecnico> findByUsuarioId(Long usuarioId);
}