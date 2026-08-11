package com.repository;

import com.entity.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    Optional<Reclamo> findByNumeroSeguimiento(String numeroSeguimiento);
    List<Reclamo> findByUsuarioId(Long usuarioId);
    List<Reclamo> findByLuminariaId(Long luminariaId);
    List<Reclamo> findByEstado(String estado);
}