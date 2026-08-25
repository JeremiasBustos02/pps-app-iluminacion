package com.repository;

import com.entity.Componente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponenteRepository extends JpaRepository<Componente, Long> {
    List<Componente> findByDeletedAtIsNull();
    Optional<Componente> findByIdAndDeletedAtIsNull(Long id);
}