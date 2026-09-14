package com.repository;

import com.entity.Reparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {
    List<Reparacion> findByReclamoIdOrderByFechaDesc(Long reclamoId);
}