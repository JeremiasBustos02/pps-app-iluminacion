package com.repository;
import com.entity.Luminaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LuminariaRepository extends JpaRepository<Luminaria, Long> {
    List<Luminaria> findByDeletedAtIsNull();
    List<Luminaria> findByZonaIdAndDeletedAtIsNull(Long zonaId);
    List<Luminaria> findByEstadoAndDeletedAtIsNull(String estado);

    @Query("SELECT l FROM Luminaria l LEFT JOIN l.zona z WHERE "
            + "l.deletedAt IS NULL AND "
            + "(:estado IS NULL OR l.estado = :estado) AND "
            + "(:zonaId IS NULL OR z.id = :zonaId)")
    List<Luminaria> filtrar(@Param("estado") String estado,
                            @Param("zonaId") Long zonaId);
}