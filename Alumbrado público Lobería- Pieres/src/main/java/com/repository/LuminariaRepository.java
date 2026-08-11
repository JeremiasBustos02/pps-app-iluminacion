package com.repository;
import com.entity.Luminaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LuminariaRepository extends JpaRepository<Luminaria, Long> {
    List<Luminaria> findByDeletedAtIsNull();
    List<Luminaria> findByZonaIdAndDeletedAtIsNull(Long zonaId);
    List<Luminaria> findByEstadoAndDeletedAtIsNull(String estado);
}