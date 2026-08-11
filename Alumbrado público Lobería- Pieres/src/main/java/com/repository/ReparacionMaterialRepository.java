package com.repository;

import com.entity.ReparacionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReparacionMaterialRepository extends JpaRepository<ReparacionMaterial, Long> {
    List<ReparacionMaterial> findByReparacionId(Long reparacionId);
}