package com.repository;

import com.entity.ReclamoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReclamoHistorialRepository extends JpaRepository<ReclamoHistorial, Long> {
    // Te va a servir para mostrarle al administrador la línea de tiempo del reclamo
    List<ReclamoHistorial> findByReclamoIdOrderByFechaCambioDesc(Long reclamoId);
}