package com.service;

import com.dto.UsuarioDTO;
import com.dto.UsuarioResponseDTO;
import com.entity.Cuadrilla;
import com.entity.CuadrillaTecnico;
import com.entity.Usuario;
import com.enums.Rol;
import com.repository.CuadrillaRepository;
import com.repository.CuadrillaTecnicoRepository;
import com.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuadrillaRepository cuadrillaRepository;

    @Autowired
    private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;

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

    public Optional<Usuario> findByDni(Long dni) {
        return usuarioRepository.findByDni(dni)
                .filter(u -> u.getDeletedAt() == null);
    }

    @Transactional
    public Usuario saveFromDTO(UsuarioDTO dto) {
        if (dto.getDni() == null) {
            throw new RuntimeException("El DNI es obligatorio");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya se encuentra registrado: " + dto.getEmail());
        }
        if (usuarioRepository.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya se encuentra registrado: " + dto.getDni());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());

        // Encriptar la contraseña antes de guardar (RF-03: nunca se guarda en texto plano)
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Asignar rol (si viene nulo, asigna VECINO por defecto)
        usuario.setRol(dto.getRol() != null ? dto.getRol() : Rol.VECINO);

        usuario.setDni(dto.getDni());
        usuario.setCelular(dto.getCelular());
        usuario.setCalle(dto.getCalle());
        usuario.setNumeroCalle(dto.getNumeroCalle());
        usuario.setReferenciaDomicilio(dto.getReferenciaDomicilio());

        Usuario guardado = usuarioRepository.save(usuario);
        sincronizarCuadrilla(guardado, dto.getCuadrillaId());
        return guardado;
    }

    @Transactional
    public Usuario updateFromDTO(Long id, UsuarioDTO dto) {
        Usuario usuario = findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setCelular(dto.getCelular());
        usuario.setCalle(dto.getCalle());
        usuario.setNumeroCalle(dto.getNumeroCalle());
        usuario.setReferenciaDomicilio(dto.getReferenciaDomicilio());

        // El DNI es único: solo se toca si cambió y no pertenece a otro usuario
        if (dto.getDni() != null && !dto.getDni().equals(usuario.getDni())) {
            if (usuarioRepository.existsByDni(dto.getDni())) {
                throw new RuntimeException("El DNI ya se encuentra registrado: " + dto.getDni());
            }
            usuario.setDni(dto.getDni());
        }

        if (dto.getRol() != null) {
            usuario.setRol(dto.getRol());
        }

        // Si se envió una nueva contraseña, actualizar el hash
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        sincronizarCuadrilla(actualizado, dto.getCuadrillaId());
        return actualizado;
    }

    @Transactional
    public void delete(Long id) {
        Usuario usuario = findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        // Baja lógica (soft delete): se conserva el registro y su historial
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Un usuario dado de baja deja de pertenecer a cualquier cuadrilla
        cuadrillaTecnicoRepository.deleteByUsuarioId(id);
    }

    // Devuelve el DTO de respuesta incluyendo la cuadrilla del técnico, si corresponde
    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        Cuadrilla cuadrilla = null;
        if (usuario.getRol() == Rol.TECNICO) {
            cuadrilla = cuadrillaTecnicoRepository.findByUsuarioId(usuario.getId()).stream()
                    .findFirst()
                    .map(CuadrillaTecnico::getCuadrilla)
                    .orElse(null);
        }
        return new UsuarioResponseDTO(usuario, cuadrilla);
    }

    /**
     * Mantiene coherente la asignación técnico–cuadrilla (RF-04):
     * - si el usuario no es TECNICO, se quita de toda cuadrilla;
     * - si es TECNICO y llega un cuadrillaId, se reasigna a esa cuadrilla;
     * - si es TECNICO y no llega cuadrillaId, se deja la asignación como está.
     */
    private void sincronizarCuadrilla(Usuario usuario, Long cuadrillaId) {
        if (usuario.getRol() != Rol.TECNICO) {
            cuadrillaTecnicoRepository.deleteByUsuarioId(usuario.getId());
            return;
        }

        if (cuadrillaId == null) {
            return;
        }

        Cuadrilla cuadrilla = cuadrillaRepository.findById(cuadrillaId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Cuadrilla no encontrada: " + cuadrillaId));

        boolean yaAsignadoAEsaCuadrilla = cuadrillaTecnicoRepository
                .findByCuadrillaIdAndUsuarioId(cuadrillaId, usuario.getId())
                .isPresent();
        if (yaAsignadoAEsaCuadrilla) {
            return;
        }

        cuadrillaTecnicoRepository.deleteByUsuarioId(usuario.getId());

        CuadrillaTecnico asignacion = new CuadrillaTecnico();
        asignacion.setCuadrilla(cuadrilla);
        asignacion.setUsuario(usuario);
        cuadrillaTecnicoRepository.save(asignacion);
    }
}
