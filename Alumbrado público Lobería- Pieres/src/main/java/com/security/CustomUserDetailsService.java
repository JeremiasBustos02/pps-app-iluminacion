package com.security;

import com.entity.Usuario;
import com.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // El "username" de Spring Security es el DNI (RF-03: se inicia sesion con DNI y contrasena)
    @Override
    public UserDetails loadUserByUsername(String dni) throws UsernameNotFoundException {
        Long dniNumerico;
        try {
            dniNumerico = Long.valueOf(dni);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("DNI invalido: " + dni);
        }

        Usuario usuario = usuarioRepository.findByDni(dniNumerico)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new UsernameNotFoundException("DNI no registrado: " + dni));

        return User.builder()
                .username(String.valueOf(usuario.getDni()))
                .password(usuario.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
                .build();
    }
}
