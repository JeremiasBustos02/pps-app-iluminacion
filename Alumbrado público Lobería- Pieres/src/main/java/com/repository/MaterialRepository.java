package com.repository;

import com.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByDeletedAtIsNull();
    Optional<Material> findByIdAndDeletedAtIsNull(Long id);
}