package com.security;

import com.entity.Usuario;
import com.enums.Rol;
import com.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService (RF-03): login por DNI")
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private Usuario usuario(Rol rol, LocalDateTime deletedAt) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setDni(30111222L);
        u.setEmail("admin@loberia.gob.ar");
        u.setPasswordHash("$2a$10$hashDePrueba");
        u.setRol(rol);
        u.setDeletedAt(deletedAt);
        return u;
    }

    @Test
    @DisplayName("carga el usuario por DNI y mapea el rol a ROLE_<rol>")
    void cargaUsuarioYMapeaRol() {
        when(usuarioRepository.findByDni(30111222L)).thenReturn(Optional.of(usuario(Rol.ADMINISTRADOR, null)));

        UserDetails details = service.loadUserByUsername("30111222");

        assertThat(details.getUsername()).isEqualTo("30111222");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashDePrueba");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    @DisplayName("lanza UsernameNotFoundException si el DNI no existe")
    void dniInexistente() {
        when(usuarioRepository.findByDni(99999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("99999999"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("un usuario dado de baja (soft delete) no puede iniciar sesión")
    void usuarioDadoDeBajaNoIngresa() {
        when(usuarioRepository.findByDni(30111222L))
                .thenReturn(Optional.of(usuario(Rol.TECNICO, LocalDateTime.now())));

        assertThatThrownBy(() -> service.loadUserByUsername("30111222"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("un DNI no numérico se rechaza sin romper")
    void dniNoNumerico() {
        assertThatThrownBy(() -> service.loadUserByUsername("abc"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
