package com.repository;

import com.entity.Cuadrilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuadrillaRepository extends JpaRepository<Cuadrilla, Long> {
    List<Cuadrilla> findByDeletedAtIsNull();
}