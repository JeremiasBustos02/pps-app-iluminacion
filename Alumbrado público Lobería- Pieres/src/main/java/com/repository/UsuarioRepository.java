package com.repository;

import com.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);
    List<Usuario> findByRolAndDeletedAtIsNull(String rol);
    List<Usuario> findByDeletedAtIsNull();
}