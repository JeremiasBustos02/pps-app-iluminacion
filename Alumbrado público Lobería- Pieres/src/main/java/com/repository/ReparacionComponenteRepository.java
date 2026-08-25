package com.repository;

import com.entity.ReparacionComponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReparacionComponenteRepository extends JpaRepository<ReparacionComponente, Long> {
    List<ReparacionComponente> findByReparacionId(Long reparacionId);
}