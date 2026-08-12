package com.service;

import com.dto.UsuarioDTO;
import com.entity.Usuario;
import com.enums.Rol;
import com.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> findAll() {
        return usuarioRepository.findByDeletedAtIsNull();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null);
    }

    public Usuario saveFromDTO(UsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya se encuentra registrado: " + dto.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());

        // Encriptar la contraseña antes de guardar
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Asignar rol (si viene nulo, asigna VECINO por defecto)
        usuario.setRol(dto.getRol() != null ? dto.getRol() : Rol.VECINO);

        usuario.setDni(dto.getDni());
        usuario.setCelular(dto.getCelular());
        usuario.setCalle(dto.getCalle());
        usuario.setNumeroCalle(dto.getNumeroCalle());
        usuario.setReferenciaDomicilio(dto.getReferenciaDomicilio());

        return usuarioRepository.save(usuario);
    }

    public Usuario updateFromDTO(Long id, UsuarioDTO dto) {
        Usuario usuario = findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setDni(dto.getDni());
        usuario.setCelular(dto.getCelular());
        usuario.setCalle(dto.getCalle());
        usuario.setNumeroCalle(dto.getNumeroCalle());
        usuario.setReferenciaDomicilio(dto.getReferenciaDomicilio());

        if (dto.getRol() != null) {
            usuario.setRol(dto.getRol());
        }

        // Si se envió una nueva contraseña, actualizar el hash
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return usuarioRepository.save(usuario);
    }

    public void delete(Long id) {
        Usuario usuario = findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}