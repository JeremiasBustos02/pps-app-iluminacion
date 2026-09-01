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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService: RF-03 (hash de contraseña) y RF-04 (alta/baja y cuadrilla del técnico)")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CuadrillaRepository cuadrillaRepository;
    @Mock private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioService usuarioService;

    // --- helpers ---------------------------------------------------------------

    private UsuarioDTO dtoBase() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Juan Perez");
        dto.setEmail("juan@vecino.com");
        dto.setPassword("secreta123");
        dto.setDni(30111222L);
        return dto;
    }

    private Usuario usuarioExistente(Long id, Rol rol, Long dni) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Original");
        u.setEmail("original@mail.com");
        u.setPasswordHash("HASH_ORIGINAL");
        u.setRol(rol);
        u.setDni(dni);
        return u;
    }

    private Cuadrilla cuadrilla(Long id, String nombre) {
        Cuadrilla c = new Cuadrilla();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    /** Simula el save de JPA devolviendo la misma entidad y asignándole id si no tiene. */
    private void stubSaveUsuarioDevolviendoId(Long idAsignado) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(idAsignado);
            }
            return u;
        });
    }

    // --- RF-03: la contraseña se guarda hasheada ------------------------------

    @Nested
    @DisplayName("saveFromDTO / RF-03")
    class Rf03 {

        @Test
        @DisplayName("guarda al vecino con la contraseña hasheada, nunca en texto plano")
        void guardaVecinoConPasswordHasheada() {
            UsuarioDTO dto = dtoBase();
            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(false);
            when(passwordEncoder.encode("secreta123")).thenReturn("HASH_BCRYPT");
            stubSaveUsuarioDevolviendoId(1L);

            usuarioService.saveFromDTO(dto);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario guardado = captor.getValue();

            assertThat(guardado.getPasswordHash()).isEqualTo("HASH_BCRYPT");
            assertThat(guardado.getPasswordHash()).isNotEqualTo("secreta123");
            assertThat(guardado.getRol()).isEqualTo(Rol.VECINO); // rol nulo en el DTO => VECINO
        }

        @Test
        @DisplayName("rechaza el alta si no se envía DNI (es la credencial de acceso)")
        void rechazaDniNulo() {
            UsuarioDTO dto = dtoBase();
            dto.setDni(null);

            assertThatThrownBy(() -> usuarioService.saveFromDTO(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DNI");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza el alta si el email ya existe")
        void rechazaEmailDuplicado() {
            UsuarioDTO dto = dtoBase();
            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.saveFromDTO(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("email");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza el alta si el DNI ya existe")
        void rechazaDniDuplicado() {
            UsuarioDTO dto = dtoBase();
            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.saveFromDTO(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DNI");

            verify(usuarioRepository, never()).save(any());
        }
    }

    // --- RF-04: alta / edición / baja y asignación de cuadrilla --------------

    @Nested
    @DisplayName("RF-04 / asignación técnico–cuadrilla")
    class Rf04Cuadrilla {

        @Test
        @DisplayName("al dar de alta un TECNICO con cuadrillaId lo vincula a esa cuadrilla")
        void asignaTecnicoACuadrillaEnElAlta() {
            UsuarioDTO dto = dtoBase();
            dto.setRol(Rol.TECNICO);
            dto.setCuadrillaId(5L);

            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("HASH");
            stubSaveUsuarioDevolviendoId(10L);
            when(cuadrillaRepository.findById(5L)).thenReturn(Optional.of(cuadrilla(5L, "Cuadrilla Norte")));
            when(cuadrillaTecnicoRepository.findByCuadrillaIdAndUsuarioId(5L, 10L)).thenReturn(Optional.empty());
            when(cuadrillaTecnicoRepository.save(any(CuadrillaTecnico.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.saveFromDTO(dto);

            ArgumentCaptor<CuadrillaTecnico> captor = ArgumentCaptor.forClass(CuadrillaTecnico.class);
            verify(cuadrillaTecnicoRepository).save(captor.capture());
            assertThat(captor.getValue().getCuadrilla().getId()).isEqualTo(5L);
            assertThat(captor.getValue().getUsuario().getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("si el técnico ya pertenece a esa cuadrilla no se crea un vínculo duplicado")
        void noDuplicaAsignacion() {
            UsuarioDTO dto = dtoBase();
            dto.setRol(Rol.TECNICO);
            dto.setCuadrillaId(5L);

            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("HASH");
            stubSaveUsuarioDevolviendoId(10L);
            when(cuadrillaRepository.findById(5L)).thenReturn(Optional.of(cuadrilla(5L, "Cuadrilla Norte")));
            when(cuadrillaTecnicoRepository.findByCuadrillaIdAndUsuarioId(5L, 10L))
                    .thenReturn(Optional.of(new CuadrillaTecnico()));

            usuarioService.saveFromDTO(dto);

            verify(cuadrillaTecnicoRepository, never()).save(any(CuadrillaTecnico.class));
            verify(cuadrillaTecnicoRepository, never()).deleteByUsuarioId(anyLong());
        }

        @Test
        @DisplayName("asignar a una cuadrilla inexistente lanza error")
        void cuadrillaInexistente() {
            UsuarioDTO dto = dtoBase();
            dto.setRol(Rol.TECNICO);
            dto.setCuadrillaId(99L);

            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("HASH");
            stubSaveUsuarioDevolviendoId(10L);
            when(cuadrillaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.saveFromDTO(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cuadrilla");
        }

        @Test
        @DisplayName("un usuario que NO es técnico queda desvinculado de toda cuadrilla")
        void noTecnicoSeDesvincula() {
            UsuarioDTO dto = dtoBase();
            dto.setRol(Rol.VECINO);

            when(usuarioRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(usuarioRepository.existsByDni(dto.getDni())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("HASH");
            stubSaveUsuarioDevolviendoId(10L);

            usuarioService.saveFromDTO(dto);

            verify(cuadrillaTecnicoRepository).deleteByUsuarioId(10L);
            verify(cuadrillaTecnicoRepository, never()).save(any(CuadrillaTecnico.class));
        }

        @Test
        @DisplayName("al cambiar el rol a TECNICO en una edición se asigna la cuadrilla indicada")
        void edicionCambiaRolAeTecnicoYAsigna() {
            Usuario existente = usuarioExistente(7L, Rol.VECINO, 30111222L);
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cuadrillaRepository.findById(3L)).thenReturn(Optional.of(cuadrilla(3L, "Cuadrilla Sur")));
            when(cuadrillaTecnicoRepository.findByCuadrillaIdAndUsuarioId(3L, 7L)).thenReturn(Optional.empty());
            when(cuadrillaTecnicoRepository.save(any(CuadrillaTecnico.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO dto = dtoBase();
            dto.setRol(Rol.TECNICO);
            dto.setCuadrillaId(3L);

            usuarioService.updateFromDTO(7L, dto);

            ArgumentCaptor<CuadrillaTecnico> captor = ArgumentCaptor.forClass(CuadrillaTecnico.class);
            verify(cuadrillaTecnicoRepository).save(captor.capture());
            assertThat(captor.getValue().getCuadrilla().getId()).isEqualTo(3L);
            assertThat(existente.getRol()).isEqualTo(Rol.TECNICO);
        }
    }

    @Nested
    @DisplayName("RF-04 / edición y baja")
    class Rf04AltaBaja {

        @Test
        @DisplayName("rehashea la contraseña sólo si se envía una nueva")
        void rehasheaPasswordSoloSiViene() {
            Usuario existente = usuarioExistente(1L, Rol.VECINO, 30111222L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(passwordEncoder.encode("claveNueva")).thenReturn("HASH_NUEVO");

            UsuarioDTO dto = dtoBase();
            dto.setPassword("claveNueva");
            usuarioService.updateFromDTO(1L, dto);

            assertThat(existente.getPasswordHash()).isEqualTo("HASH_NUEVO");
        }

        @Test
        @DisplayName("no toca la contraseña si el campo viene vacío")
        void noRehasheaSiPasswordVacia() {
            Usuario existente = usuarioExistente(1L, Rol.VECINO, 30111222L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO dto = dtoBase();
            dto.setPassword("   ");
            usuarioService.updateFromDTO(1L, dto);

            assertThat(existente.getPasswordHash()).isEqualTo("HASH_ORIGINAL");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("rechaza la edición si el nuevo DNI ya pertenece a otro usuario")
        void rechazaDniDuplicadoEnEdicion() {
            Usuario existente = usuarioExistente(1L, Rol.VECINO, 30111222L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.existsByDni(40999888L)).thenReturn(true);

            UsuarioDTO dto = dtoBase();
            dto.setDni(40999888L);

            assertThatThrownBy(() -> usuarioService.updateFromDTO(1L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DNI");
        }

        @Test
        @DisplayName("la baja es lógica (soft delete): setea deletedAt y desvincula de cuadrillas")
        void bajaEsLogica() {
            Usuario existente = usuarioExistente(1L, Rol.TECNICO, 30111222L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.delete(1L);

            assertThat(existente.getDeletedAt()).isNotNull();
            verify(usuarioRepository).save(existente);
            verify(usuarioRepository, never()).delete(any());
            verify(cuadrillaTecnicoRepository).deleteByUsuarioId(1L);
        }

        @Test
        @DisplayName("dar de baja un usuario inexistente lanza error")
        void bajaDeInexistente() {
            when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.delete(9L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("toResponseDTO")
    class ToResponseDto {

        @Test
        @DisplayName("incluye la cuadrilla cuando el usuario es TECNICO")
        void incluyeCuadrillaParaTecnico() {
            Usuario tecnico = usuarioExistente(4L, Rol.TECNICO, 30111222L);
            CuadrillaTecnico ct = new CuadrillaTecnico();
            ct.setCuadrilla(cuadrilla(2L, "Cuadrilla A"));
            ct.setUsuario(tecnico);
            when(cuadrillaTecnicoRepository.findByUsuarioId(4L)).thenReturn(List.of(ct));

            UsuarioResponseDTO dto = usuarioService.toResponseDTO(tecnico);

            assertThat(dto.getCuadrillaId()).isEqualTo(2L);
            assertThat(dto.getCuadrillaNombre()).isEqualTo("Cuadrilla A");
        }

        @Test
        @DisplayName("no consulta ni informa cuadrilla para un vecino")
        void sinCuadrillaParaVecino() {
            Usuario vecino = usuarioExistente(4L, Rol.VECINO, 30111222L);

            UsuarioResponseDTO dto = usuarioService.toResponseDTO(vecino);

            assertThat(dto.getCuadrillaId()).isNull();
            assertThat(dto.getCuadrillaNombre()).isNull();
            verify(cuadrillaTecnicoRepository, never()).findByUsuarioId(anyLong());
        }
    }

    @Test
    @DisplayName("findByDni ignora a los usuarios dados de baja")
    void findByDniIgnoraBajas() {
        Usuario baja = usuarioExistente(1L, Rol.VECINO, 30111222L);
        baja.setDeletedAt(LocalDateTime.now());
        when(usuarioRepository.findByDni(30111222L)).thenReturn(Optional.of(baja));

        assertThat(usuarioService.findByDni(30111222L)).isEmpty();
    }
}
