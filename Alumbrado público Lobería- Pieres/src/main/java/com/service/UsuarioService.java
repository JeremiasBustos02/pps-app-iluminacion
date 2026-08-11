package com.service;

import com.entity.Usuario;
import com.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Usuario> findAll() {
        return usuarioRepository.findByDeletedAtIsNull();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id).filter(u -> u.getDeletedAt() == null);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmailAndDeletedAtIsNull(email);
    }

    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public Usuario update(Long id, Usuario details) {
        Usuario usuario = findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setNombre(details.getNombre());
        usuario.setCelular(details.getCelular());
        usuario.setCalle(details.getCalle());
        usuario.setNumeroCalle(details.getNumeroCalle());
        usuario.setReferenciaDomicilio(details.getReferenciaDomicilio());
        return usuarioRepository.save(usuario);
    }

    public void delete(Long id) {
        Usuario usuario = findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}